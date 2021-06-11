from server.database import insert_wrong_sign
import socket
import json
from hashlib import sha256, sha_256, sha3_256
import hmac
import sqlite3
import conf
from custom_logger import warning, info
from database import initialize_db, duplicated_nonce, insert_new_nonce, insert_no_attack, insert_reply_attack, insert_integrity_attack, insert_brute_force_attack, select_attacked, select_all_responses, ATTACK_INTEGRITY, ATTACK_REPLY, ATTACK_BRUTE_FORCE
import datetime
import ssl
import os
from Crypto.Signature import PKCS1_v1_5
from Crypto.Hash import SHA256
from Crypto.PublicKey import RSA
import base64

HOST = conf.SERVER_IP
PORT = conf.SERVER_PORT
DEBUG_MODE = conf.DEBUG_MODE
ALWAYS_CORRECT = DEBUG_MODE and conf.ALWAYS_CORRECT
FAST_LOOP = DEBUG_MODE and conf.FAST_LOOP
SECRET = conf.SECRET
SCAN_DIRECTORY = conf.SCAN_DIRECTORY
REPORT_DIRECTORY = conf.REPORT_DIRECTORY
FRECUENCY = conf.HOUR_FRECUENCY
ENCODING = 'utf-8'
CERT = conf.CERT
KEY = conf.KEY
NONCE_DB = 'nonce.db'


class Server:
    # 256 bits random number
    key = 108079546209274483481442683641105470668825844172663843934775892731209928221929

    def __init__(self, host='127.0.0.1', port=55333, cert_file="./dummy_certs/certificate.pem",
                 key_file="./dummy_certs/key.pem"):
        self.host = host
        self.port = port
        self.cert_file = os.path.abspath(cert_file)
        self.key_file = os.path.abspath(key_file)
        self.db = sqlite3.connect(NONCE_DB)

        # UNCOMMENT FOR TLS
        # self.context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        # self.context.load_cert_chain(cert_file, key_file)
        # self.context.set_ciphers("ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-CHACHA20-POLY1305-SHA256:ECDHE-RSA-CHACHA20-POLY1305-SHA256:ECDHE-ECDSA-AES-256-GCM-SHA384:ECDHE-RSA-AES-256-GCM-SHA384")
        initialize_db(self.db)

    def run(self):
        while True:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM, 0) as sock:
                sock.bind((self.host, self.port))
                print('Port:' + str(self.port))
                sock.listen(10)

                # UNCOMMENT FOR TLS
                # with self.context.wrap_socket(sock, server_side=True) as ssock:

                while True:
                    
                    # UNCOMMENT FOR TLS
                    # conn, addr = ssock.accept()
                    conn, addr = sock.accept()
                    with conn:
                        if DEBUG_MODE:
                            print('Connected by', addr)
                        while True:
                            data = conn.recv(1024)

                            if not data:
                                break
                            if DEBUG_MODE:
                                print(data)

                            loaded_data = json.loads(data)

                            message = loaded_data["message"]
                            nonce = loaded_data["nonce"]
                            hmac = loaded_data["hmac"]
                            messageSign = loaded_data["messageSign"]
                            more_nonces = duplicated_nonce(self.db, nonce)
                            generated_hmac = generate_hmac(self.key, message, nonce)

                            now = datetime.datetime.now()
                            since = datetime.datetime(now.year, now.month, now.day).timestamp()
                            hour = datetime.timedelta(hours=4)
                            moment = since - hour
                            thread_db = sqlite3.connect(NONCE_DB)
                            requests = select_all_responses(thread_db, moment)

                            messageEnc = SHA256.new(message.encode())
                            signature = PKCS1_v1_5.new(self.key).sign(messageEnc)
                            resultSignature = base64.b64encode(signature).decode()
                            

                            if more_nonces:
                                warning(f'VERIFICATION FAILURE: Reply attack detected')
                                insert_reply_attack(self.db)
                                response = {"RESPONSE": "Conection failed: This message have been duplicated"}
                            elif hmac != generated_hmac:
                                warning(f'VERIFICATION FAILURE: Integrity have been compromised')
                                insert_integrity_attack(self.db)
                                response = {"RESPONSE": "Conection failed: Message integrity have been compromised"}
                            elif requests > 3:
                                warning(f'SERVER FAILURE: Server is being attacked. Please try later')
                                insert_brute_force_attack(self.db)
                                response = {"RESPONSE": "Conection failed: Server is being attacked"}
                            elif messageSign != resultSignature:
                                warning(f'SERVER FAILURE: Server could not verify the message sign')
                                insert_wrong_sign(self.db)
                                response = {"RESPONSE": "Wrong signature: Client and server sign are not the same"}
                            else:
                                insert_new_nonce(self.db, nonce)
                                insert_no_attack(self.db)
                                warning(f'ACCEPTED: No problems detected')
                                response = {"RESPONSE": " PETICION OK"}

                            if DEBUG_MODE:
                                print(str(response))
                            dumped_response = json.dumps(response)
                            conn.sendall(bytes(dumped_response, encoding=ENCODING))

                sock.close()

    @staticmethod
    def reports():
        thread_db = sqlite3.connect(NONCE_DB)
        now = datetime.datetime.now()
        file_name = now.strftime("%d_%m_%Y")
        f = open(REPORT_DIRECTORY + '/report_' + file_name + '.txt','w')
        f.write("\n--------------------------------------------------\n")
        f.write("DAY " + now.strftime("%d %m %Y") + "\n")

        since = datetime.datetime(now.year, now.month, now.day)

        month = datetime.timedelta(days=30)
        two_months = datetime.timedelta(days=60)
        three_months = datetime.timedelta(days=90)
        p1 = since - three_months
        p2 = since - two_months
        p3 = since - month
        n_all1 = select_all_responses(thread_db, p1)
        n_reply1 = select_attacked(thread_db, ATTACK_REPLY,p1)
        n_failed_integrity1 = select_attacked(thread_db, ATTACK_INTEGRITY, p1)
        n_correct1 = n_all1 - n_reply1 - n_failed_integrity1
        n_p1 = n_correct1 / n_all1

        n_all2 = select_all_responses(thread_db, p2)
        n_reply2 = select_attacked(thread_db, ATTACK_REPLY,p2)
        n_failed_integrity2 = select_attacked(thread_db, ATTACK_INTEGRITY, p2)
        n_correct2 = n_all2 - n_reply2 - n_failed_integrity2
        n_p2 = n_correct2 / n_all2

        n_all3 = select_all_responses(thread_db, p3)
        n_reply3 = select_attacked(thread_db, ATTACK_REPLY, p3)
        n_failed_integrity3 = select_attacked(thread_db, ATTACK_INTEGRITY, p3)
        n_correct3 = n_all3 - n_reply3 - n_failed_integrity3
        n_p3 = n_correct3 / n_all3

        trend = "NO TREND"

        if (n_all1 == 0 | n_all2 == 0):
            trend = "0"
        elif (n_p3 > n_p1 & n_p3 > n_p2) | (n_p3 > n_p1 & n_p3 == n_p2) | (n_p3 == n_p1 & n_p3 > n_p2):
            trend = "+"
        elif (n_p3 < n_p1 | n_p3 < n_p2):
            trend = "-"
        elif (n_p3 == n_p1 & n_p3 == n_p2):
            trend = "0"

        print('Before day')

        n_all = select_all_responses(thread_db, since)
        n_reply = select_attacked(thread_db, ATTACK_REPLY,since)
        n_failed_integrity = select_attacked(thread_db, ATTACK_INTEGRITY, since)
        suffix_msg = " messages"
        n_correct = n_all - n_reply - n_failed_integrity
        f.write("TOTAL: " + str(n_all) + suffix_msg + "\n")
        f.write("   - TREND: " + str(trend) + "\n")
        f.write("   - CORRECT: " + str(n_correct) + suffix_msg + "\n")
        f.write("   - REPLY ATTACK: " + str(n_reply) + suffix_msg + "\n")
        f.write("   - FAILED INTEGRITY: " + str(n_failed_integrity) + suffix_msg + "\n\n")

        if n_all==0:
            n_all=1
        reply_kpi = n_reply/n_all
        failed_kpi = n_failed_integrity/n_all
        correct_kpi = n_correct/n_all
        suffix_ptg = " %"
        f.write("KPI \n")
        f.write("   - TREND: " + str(trend) + "\n")
        f.write("   - CORRECT: " + str(correct_kpi) + suffix_ptg + "\n")
        f.write("   - REPLY ATTACK: " + str(reply_kpi) + suffix_ptg + "\n")
        f.write("   - FAILED INTEGRITY: " + str(failed_kpi) + suffix_ptg + "\n")
        f.write("\n--------------------------------------------------\n\n")


def generate_hmac(key, message, nonce):
    encoded_key = repr(key).encode(ENCODING)
    body = str(message) + nonce
    raw_body = body.encode(ENCODING)
    hashed = hmac.new(encoded_key, raw_body, sha_256)
    return hashed.hexdigest()


if __name__ == "__main__":
    server = Server(HOST, PORT, CERT, KEY)
    server.run()


