#!/usr/bin/env bash

function die() {
  echo $*
  exit 1
}

${FIRETWEET_DOWNLOADS:?"FIRETWEET_DOWNLOADS must be set to the location of the firetweet/downloads repository"}

#gradle || die "Could not build"
s3cmd put -P bin/firetweet.apk s3://firetweet || die "Could not upload binary to S3?"
cp bin/firetweet.apk $FIRETWEET_DOWNLOADS/ || die "Could not copy to downloads dir"
cd $FIRETWEET_DOWNLOADS || die "Could not move to downloads dir"
git add firetweet.apk || die "Could not add apk?"
git commit -m "Updated version" 
git push origin master || die "Could not push new FireTweet version!"
cd -
