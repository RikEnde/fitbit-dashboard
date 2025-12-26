#!/bin/zsh

mkdir -p output
#max_jobs=3

for year in {2016..2025}; do
#  while (( $(jobs -r | wc -l) >= max_jobs )); do
#    wait -n
#  done
#
#  {
    echo "Exporting heart rate data for $year..."
    curl -sS --fail "http://localhost:8080/api/export/heartrate?from=${year}-01-01T00:00:00&to=${year}-12-31T23:59:59" -o "output/heartrate-${year}.xml" &&
    ls -lh "output/heartrate-${year}.xml"
#  } &
done

#wait

