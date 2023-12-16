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

def save_physicalstate(physicalstate, db_config):
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
        INSERT INTO physicalstate (device_id, device_timestamp, timestamp, temperature, energy)
        VALUES (%s, %s, %s, %s, %s)
        RETURNING id;
        """
        data = (
            physicalstate['device_id'],
            physicalstate['device_timestamp'],
            physicalstate['timestamp'],
            physicalstate['temperature'],
            physicalstate['energy']
        )
        cursor.execute(query, data)
        id = cursor.fetchone()[0] # type: ignore
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
        physicalevent = request.json
        logging.info('request: ' + str(physicalevent))
        physicalstate = {}
        physicalstate['device_id'] = physicalevent['device_id']
        physicalstate['device_timestamp'] = int(physicalevent['timestamp'])
        physicalstate['timestamp'] = int(time.time() * 1000)
        physicalstate['temperature'] = physicalevent['data']['iot.sensor.temperature']
        physicalstate['energy'] = physicalevent['data']['iot.sensor.energy']
        physicalstate['id'] = save_physicalstate(physicalstate, get_db_config())
        logging.info('response: ' + str(physicalstate))
        return json.dumps(physicalstate)
    except Exception as e:
        logging.error(e)
        return json.dumps({'error': str(e)}), 500