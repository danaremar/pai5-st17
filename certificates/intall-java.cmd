"C:\Program Files\Java\jdk-11.0.1\bin\keytool.exe" -import -trustcacerts -alias root -file "C:\Users\elsen\Documents\GitHub\pai5-st17\certificates\certificate.pem" -keystore "C:\Program Files\Java\jdk-11.0.1\lib\security\cacerts" -storepass changeit

"C:\Program Files\Java\jdk-11.0.1\bin\keytool.exe" -list -keystore "C:\Program Files\Java\jdk-11.0.1\lib\security\cacerts"

"C:\Program Files\Java\jdk-11.0.1\bin\keytool.exe" -importcert -file "C:\Users\elsen\Documents\GitHub\pai5-st17\certificates\certificate.pem" -keystore "C:\Users\elsen\Documents\GitHub\pai5-st17\certificates\certificate.jks" -storepass jkspass -alias st17 -keypass st17