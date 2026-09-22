# JBM tv — by Céleste Patri-land

Application IPTV (M3U / Xtream Codes) pour téléphone et **Android TV**, en mode paysage,
avec un lecteur vidéo natif (ExoPlayer) pour un démarrage rapide des chaînes.

## Générer l'APK avec GitHub

1. Créez un dépôt GitHub et envoyez-y tous les fichiers de ce dossier, à la racine.
   Renommez ensuite `build-apk.yml` en `.github/workflows/build-apk.yml`
   (ouvrez le fichier dans GitHub, touchez le crayon, changez son nom en incluant
   le chemin complet, puis validez).
2. Ouvrez l'onglet **Actions** du dépôt. La construction démarre automatiquement ;
   sinon choisissez « Construire l'APK JBM tv » puis **Run workflow**.
3. Une fois l'exécution terminée (5 minutes environ), téléchargez **JBM-tv-apk** en bas
   de la page, décompressez-le et installez `app-debug.apk` sur le téléphone ou la box.

## Ce que fait cette version

- **Interface horizontale (paysage)**, avec les mêmes écrans que Smarters IPTV Pro :
  accueil avec grandes tuiles, TV en direct avec colonnes catégories/chaînes/aperçu,
  favoris, lock (verrouillage par chaîne), historique, réglages.
- **Écran de lancement** : photo de l'équipe (`resources/team.jpg`) en fond, avec le titre
  « JBM tv by Céleste Patri-land ». Il n'y a plus de bouton pour ajouter une photo depuis
  l'application : la photo est intégrée une fois pour toutes à la construction.
- **Lecteur vidéo natif (ExoPlayer)** sur Android : contrairement à un lecteur web classique,
  il lit directement les flux `.ts` que renvoient la plupart des serveurs Xtream, avec un
  tampon minimal, ce qui corrige les chaînes très longues à démarrer.
- **Compatible Android TV** : bannière, catégorie de lancement Leanback, écran toujours
  en mode paysage, navigation possible à la télécommande (flèches + OK + Retour).

## Remplacer la photo de l'équipe

Remplacez `resources/team.jpg` par une autre image avant de lancer la construction.

## Contenu

- `index_template.html` : l'application (fusionnée avec le logo lors de la construction)
- `resources/logo.png`, `resources/team.jpg` : logo et photo de lancement
- `MainActivity.java`, `JbmPlayerPlugin.java` : lecteur vidéo natif Android (ExoPlayer)
- `patch_android.py` : icônes, bannière Android TV, manifeste, intégration du lecteur natif
- `prepare-www.js` : assemble le dossier `www/` avant la construction Android
- `.github/workflows/build-apk.yml` (une fois renommé) : construction automatique de l'APK
