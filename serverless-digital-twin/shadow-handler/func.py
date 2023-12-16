from flask import request
import logging
import json
import os
import psycopg2
import time

def get_db_config():
    config = {}
    keys = ['database', 'user', 'password', 'host', 'port']
    for key in keys:
        path = '/configs/default/postgres/' + key
        if os.path.exists(path):
            with open(path, 'r') as f:
                config[key] = f.read()
    logging.info('db config: ' + str(config))
    return config

def save_digitalstate(digitalstate, db_config):
    try:
        conn = psycopg2.connect(
            dbname=db_config['database'],
            user=db_config['user'],
            password=db_config['password'],
            host=db_config['host'],
            port=int(db_config['port']),
        )
        cursor = conn.cursor()
        query = """
        INSERT INTO digitalstate (physicalstate_id, device_id, timestamp, temperature, energy)
        VALUES (%s, %s, %s, %s, %s)
        RETURNING id;
        """
        data = (
            digitalstate['physicalstate_id'],
            digitalstate['device_id'],
            digitalstate['timestamp'],
            digitalstate['temperature'],
            digitalstate['energy']
        )
        cursor.execute(query, data)
        id = cursor.fetchone()[0]
        conn.commit()
        cursor.close()
        conn.close()
        return id
    except (Exception, psycopg2.Error) as error:
        raise error

def main():
    try:
        logging.basicConfig(
            level=logging.DEBUG,
            format='%(asctime)s %(levelname)s %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        physicalstate = request.json
        logging.info('request: ' + str(physicalstate))
        pendingdigitalstate = {}
        pendingdigitalstate['physicalstate_id'] = physicalstate['id']
        pendingdigitalstate['device_id'] = physicalstate['device_id']
        pendingdigitalstate['temperature'] = physicalstate['temperature']
        pendingdigitalstate['energy'] = physicalstate['energy']
        pendingdigitalstate['timestamp'] = int(time.time() * 1000)
        pendingdigitalstate['id'] = save_digitalstate(pendingdigitalstate, get_db_config())
        logging.info('response: ' + str(pendingdigitalstate))
        return json.dumps(pendingdigitalstate)
    except Exception as e:
        logging.error(e)
        return json.dumps({'error': str(e)}), 500