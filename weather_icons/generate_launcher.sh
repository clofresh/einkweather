#!/bin/bash

base_dir="generated/launcher"
mkdir -p $base_dir
src=AdamWhitcroft-Climacons-0d1147a/SVG/Cloud-Rain-Sun.svg
dest=../res/drawable/ic_launcher.png
base_size=48
scale=1
dim=$(echo "$scale * $base_size" | bc)
dim=${dim%.0}
dims="${dim}x${dim}"
echo "Converting $src $dims and writing to $dest"
convert -trim +repage -background none -density 1200 -resize $dims -gravity Center -extent $dims "$src" "$dest"

