#!/bin/zsh

mkdir -p output

dump_year() {
  type=$1
  year=$2
  echo "Exporting heart rate data for $year..."
  curl -sS --fail "http://localhost:8080/api/export/${type}?from=${year}-01-01T00:00:00&to=${year}-12-31T23:59:59" -o "output/heartrate-${year}.xml" &&
  ls -lh "output/${type}-${year}.xml"
}

dump_all() {
  for year in {2016..2025}; do
    dump_year "heartrate" "$year"
  done
}

dump_quarter() {
  type=$1
  year=$2
  quarter=$3
  case $quarter in
    1) from="${year}-01-01T00:00:00"; to="${year}-03-31T23:59:59" ;;
    2) from="${year}-04-01T00:00:00"; to="${year}-06-30T23:59:59" ;;
    3) from="${year}-07-01T00:00:00"; to="${year}-09-30T23:59:59" ;;
    4) from="${year}-10-01T00:00:00"; to="${year}-12-31T23:59:59" ;;
  esac
  echo "Exporting ${type} for ${year} Q${quarter}..."
  curl -sS --fail "http://localhost:8080/api/export/${type}?from=${from}&to=${to}" -o "output/${type}-${year}-Q${quarter}.xml" &&
  ls -lh "output/${type}-${year}-Q${quarter}.xml"
}

dump_year_quarters() {
  type=$1
  year=$2
  for q in 1 2 3 4; do
    dump_quarter "$type" "$year" "$q"
  done
}
