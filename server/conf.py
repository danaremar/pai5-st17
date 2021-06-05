import os
import configparser

config = configparser.SafeConfigParser()

config.read("conf.cfg")

SERVER_IP = config.get('server', 'IP')
SERVER_PORT = int(config.get('server', 'PORT'))
ALWAYS_CORRECT = "True" == config.get('debug', 'ALWAYS_CORRECT')
DEBUG_MODE = "True" == config.get('debug', 'DEBUG_MODE')
FAST_LOOP = "True" == config.get('debug', 'FAST_LOOP')
SECRET = int(config.get('keys', 'SECRET'))
CERT = config.get('keys', 'CERT')
KEY = config.get('keys', 'KEY')
SCAN_DIRECTORY = config.get('path','SCAN_DIRECTORY')
REPORT_DIRECTORY = config.get('path','REPORT_DIRECTORY')
LOGS = config.get('path','LOGS')
HOUR_FRECUENCY = int(config.get('reports','HOUR_FRECUENCY'))