#!/bin/bash
# This script zips the WAR used in the app
WWW_SRC="www"
WWW_DST="app/src/main/assets/root.war"

cd ${WWW_SRC}
zip -r tmp.war *
cd ..

mv ${WWW_SRC}/tmp.war ${WWW_DST}

