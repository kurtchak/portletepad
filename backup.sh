project=`pwd | cut -d'/' -f 5`
file=$project"_"`date "+%Y%m%d_%H%M"`.zip
zip -rq ../backup/$file src/ WebContent/ pom.xml
#zip -req ../backup/$file src/ WebContent/ pom.xml # encrypted - with pass
echo "successfully backed up"
du -sh ../backup/$file
#scp `ls -r ../backup/$project"_"*.zip | head -n 1` xkorca02@merlin.fit.vutbr.cz:dpnew/backup/
