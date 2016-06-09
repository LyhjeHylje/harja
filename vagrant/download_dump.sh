#!/bin/sh

set -e

date="$(stat -f "%Sm" ../tietokanta/harja-stg-dump)"
AIKARAJA=7200

if [ ! "$1" = "force" ];
then
    # Jos alle 5 päivää vanha, varmistetaan että halutaanhan jatkaa
    if test `find "../tietokanta/harja-stg-dump" -mmin -$AIKARAJA`
    then
        if [ "$1" = "default" ];
        then
            echo "[$(date +"%T")] Dumppi on ladattu $date, ladataan tuoreempi. (Aikaraja on $AIKARAJA minuuttia.)"
        else
            read -p "[$(date +"%T")] Dumppi on ladattu $date, haluatko varmasti ladata uuden? [y N]" -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]
            then
                echo "[$(date +"%T")] Selvä, ladataan uusin dump."
            else
                echo "[$(date +"%T")] Tyydytään vanhaan dumppiin."
                exit 0
            fi
        fi
    else
        echo "[$(date +"%T")] Ladataan uusi staging dump."
    fi
else
    echo "[$(date +"%T")] Pakotetaan uuden dumpin lataus. Vanha oli ladattu $date"
fi

echo "[$(date +"%T")] Aloitetaan staging dumpin lataus! Hae vaikka kahvia."
echo "[$(date +"%T")] (Älä välitä 'Could not change directory to /home/tunnus: Permission denied' -virheestä)\n"

ssh harja-db1-stg "sudo -u postgres pg_dump -Fc --exclude-table-data=integraatioviesti --exclude-table-data=liite harja" > ../tietokanta/harja-stg-dump

echo "\n[$(date +"%T")] Dumppi ladattu. Puretaan dummpi .sql komennoiksi.\n"

# Jos muutat tätä, muuta sama rivi myös mount_dump.sh
vagrant ssh -c "pg_restore -Fc -C /harja-tietokanta/harja-stg-dump > /harja-tietokanta/restored-stg-dump.sql"

echo "\n[$(date +"%T")] Dumppi ladattu ja purettu!"