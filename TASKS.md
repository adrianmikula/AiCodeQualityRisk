
Note: The PNG fallbacks (40px and 80px retina) need to be generated separately since I can't create binary image files. You can generate them using:

bash
# Using Inkscape
inkscape pluginIcon.svg --export-filename=pluginIcon.png --export-width=40 --export-height=40
inkscape pluginIcon.svg --export-filename=pluginIcon_2x.png --export-width=80 --export-height=80
Or use an online SVG to PNG converter, or export from any vector graphics editor.

