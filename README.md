# AquaScape
Un jeu de trimard 2D où vous contrôlez une flaque d'eau dans un parcours

Lancement du jeu : 


javac -encoding UTF-8 -d out src/*.java
java -cp out Main

			OU

Lancement depuis la croix une fois dans le fichier main




Activation mode manette : 

.\run.ps1        (Windows)
sh run.sh        (Linux, Mac ou Git Bash)



En une seul commande : 


Powershell : 
javac -d out -cp "lib/*" -sourcepath src src/Main.java
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" Main

Ubuntu : 
javac -encoding UTF-8 -d out -cp "lib/*" -sourcepath src src/Main.java
java --enable-native-access=ALL-UNNAMED -cp "out:lib/*" Main

chmod +x run.sh test.sh
./run.sh
