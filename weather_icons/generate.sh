#!/bin/bash

mkdir -p generated
for src in $(ls AdamWhitcroft-Climacons-0d1147a/SVG/*.svg)
do
	dest=generated/$(basename "${src%.svg}.png")
	echo "Converting $src to $dest"
	convert -trim +repage -gravity Center -extent 120%x120%+0+0 -background none -density 1200 -resize 200x200 -extent 200x200 "$src" "$dest"
done

IFS="
"
for line in $(cat weather_codes.csv)
do
	code=$(echo $line | cut -d , -f 1)
	day=$(echo $line | cut -d , -f 3)
	night=$(echo $line | cut -d , -f 4)

	day_src=generated/${day}.png
	day_dest=../res/drawable/icon_${code}_d.png
	night_src=generated/${night}.png
	night_dest=../res/drawable/icon_${code}_n.png

	echo "Copying $day_src to $day_dest"
	cp "$day_src" "$day_dest"

	echo "Copying $night_src to $night_dest"
	cp "$night_src" "$night_dest"
done

