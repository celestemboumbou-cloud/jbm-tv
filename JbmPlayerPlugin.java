package com.jbmtv.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Lecteur vidéo natif (ExoPlayer / Media3) pour JBM tv.
 * Il lit les flux .ts, .m3u8, .mp4, .mkv directement, sans passer par la page web :
 * démarrage rapide et aucune restriction CORS.
 */
@CapacitorPlugin(name = "JbmPlayer")
public class JbmPlayerPlugin extends Plugin {

    private ExoPlayer player;
    private PlayerView playerView;
    private FrameLayout container;
    private TextView backBtn;
    private String currentUa = "";
    private boolean active = false;

    private void emit(String state, String message) {
        JSObject o = new JSObject();
        o.put("state", state);
        if (message != null) o.put("message", message);
        notifyListeners("state", o);
    }

    private void ensurePlayer(String ua) {
        String wanted = ua == null ? "" : ua;
        if (player != null && wanted.equals(currentUa)) return;
        if (player != null) {
            player.release();
            player = null;
        }
        currentUa = wanted;

        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000);
        if (wanted.length() > 0) http.setUserAgent(wanted);

        // Tampon minimal avant lecture : la chaîne démarre dès que possible
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(5000, 30000, 500, 1500)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        player = new ExoPlayer.Builder(getContext(),
                new DefaultRenderersFactory(getContext()).setEnableDecoderFallback(true))
                .setMediaSourceFactory(new DefaultMediaSourceFactory(getContext()).setDataSourceFactory(http))
                .setLoadControl(loadControl)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int s) {
                String st = s == Player.STATE_BUFFERING ? "buffering"
                        : s == Player.STATE_READY ? "ready"
                        : s == Player.STATE_ENDED ? "ended" : "idle";
                emit(st, null);
            }

            @Override
            public void onPlayerError(PlaybackException e) {
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition();
                    player.prepare();
                    return;
                }
                emit("error", e.getErrorCodeName());
            }
        });
    }

    private void ensureView() {
        if (container == null) {
            container = new FrameLayout(getContext());
            container.setBackgroundColor(Color.BLACK);

            playerView = new PlayerView(getContext());
            playerView.setUseController(false);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
            playerView.setFocusable(false);
            container.addView(playerView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            backBtn = new TextView(getContext());
            backBtn.setText("  \u2190  ");
            backBtn.setTextSize(26);
            backBtn.setTextColor(Color.WHITE);
            backBtn.setBackgroundColor(0x99000000);
            backBtn.setGravity(Gravity.CENTER);
            backBtn.setVisibility(View.GONE);
            backBtn.setOnClickListener(v -> notifyListeners("back", new JSObject()));
            FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            bl.gravity = Gravity.TOP | Gravity.START;
            bl.setMargins(24, 24, 0, 0);
            container.addView(backBtn, bl);

            container.setVisibility(View.GONE);
            getActivity().addContentView(container, new FrameLayout.LayoutParams(1, 1));
        }
        playerView.setPlayer(player);
    }

    private void applyLayout(int x, int y, int w, int h, boolean fs) {
        FrameLayout.LayoutParams lp;
        if (fs) {
            lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        } else {
            lp = new FrameLayout.LayoutParams(Math.max(w, 1), Math.max(h, 1));
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.leftMargin = x;
            lp.topMargin = y;
        }
        container.setLayoutParams(lp);
        playerView.setUseController(fs);
        playerView.setFocusable(fs);
        playerView.setFocusableInTouchMode(fs);
        backBtn.setVisibility(fs ? View.VISIBLE : View.GONE);
        container.setVisibility(View.VISIBLE);
        if (fs) {
            playerView.requestFocus();
            playerView.showController();
        } else if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().requestFocus();
        }
    }

    @PluginMethod
    public void play(final PluginCall call) {
        final String url = call.getString("url");
        if (url == null || url.length() == 0) {
            call.reject("url manquante");
            return;
        }
        final String ua = call.getString("userAgent", "");
        final boolean fs = Boolean.TRUE.equals(call.getBoolean("fullscreen", false));
        final int x = call.getInt("x", 0);
        final int y = call.getInt("y", 0);
        final int w = call.getInt("w", 0);
        final int h = call.getInt("h", 0);
        getActivity().runOnUiThread(() -> {
            try {
                ensurePlayer(ua);
                ensureView();
                applyLayout(x, y, w, h, fs);
                active = true;
                player.setMediaItem(MediaItem.fromUri(url));
                player.prepare();
                player.setPlayWhenReady(true);
                call.resolve();
            } catch (Exception e) {
                call.reject(String.valueOf(e.getMessage()));
            }
        });
    }

    @PluginMethod
    public void setLayout(final PluginCall call) {
        final boolean fs = Boolean.TRUE.equals(call.getBoolean("fullscreen", false));
        final int x = call.getInt("x", 0);
        final int y = call.getInt("y", 0);
        final int w = call.getInt("w", 0);
        final int h = call.getInt("h", 0);
        getActivity().runOnUiThread(() -> {
            if (container != null && active) applyLayout(x, y, w, h, fs);
            call.resolve();
        });
    }

    @PluginMethod
    public void setVisible(final PluginCall call) {
        final boolean v = Boolean.TRUE.equals(call.getBoolean("visible", true));
        getActivity().runOnUiThread(() -> {
            if (container != null && active) container.setVisibility(v ? View.VISIBLE : View.INVISIBLE);
            call.resolve();
        });
    }

    @PluginMethod
    public void stop(final PluginCall call) {
        getActivity().runOnUiThread(() -> {
            active = false;
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
            if (container != null) container.setVisibility(View.GONE);
            if (getBridge() != null && getBridge().getWebView() != null) getBridge().getWebView().requestFocus();
            call.resolve();
        });
    }

    @PluginMethod
    public void exitApp(final PluginCall call) {
        getActivity().runOnUiThread(() -> getActivity().finishAffinity());
        call.resolve();
    }

    @Override
    protected void handleOnPause() {
        if (player != null) player.setPlayWhenReady(false);
    }

    @Override
    protected void handleOnResume() {
        if (player != null && active) player.setPlayWhenReady(true);
    }

    @Override
    protected void handleOnDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
