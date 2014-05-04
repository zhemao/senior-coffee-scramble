VPS=vps.zhehaomao.com
REMOTE_APP_ROOT=/home/senior-scramble/app
NGINX_WWW_ROOT=/usr/share/nginx/www
APP_JAR=target/senior-coffee-scramble.jar

rsync --progress $APP_JAR $VPS:$REMOTE_APP_ROOT/application.jar
rsync -avz resources/public/* $VPS:$NGINX_WWW_ROOT/senior-scramble/
