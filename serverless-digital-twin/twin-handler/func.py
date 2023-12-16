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

def get_odte_config():
    config = {}
    keys = [
        'expected_update_rate', 
        'time_window', 
        'desired_timeliness',
        'threshold'
    ]
    for key in keys:
        path = '/configs/default/odte/' + key
        if os.path.exists(path):
            with open(path, 'r') as f:
                config[key] = float(f.read())
    logging.info('odte config: ' + str(config))
    return config

def get_odte_availability():
    return 1.0

def get_odte_reliability(received_updates, expected_updates):
    reliability = received_updates / expected_updates
    if reliability > 1.0:
        return 1.0
    else:
        return reliability

def get_odte_timeliness(obs, desired_timeliness):
    if len(obs) == 0:
        return 0.0
    elapsed_times = [item['elapsed_time'] for item in obs]
    return sum(i <= desired_timeliness * 1000 for i in elapsed_times) / len(obs)

def get_odte(digitalstate, db_config, odte_config):
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
        SELECT
            digitalstate.physicalstate_id as physicalstate_id,
            digitalstate.id as digitalstate_id,
            physicalstate.device_id as device_id,
            physicalstate.device_timestamp as physical_timestamp,
            digitalstate.timestamp as digital_timestamp
        FROM digitalstate
        INNER JOIN physicalstate ON digitalstate.physicalstate_id = physicalstate.id
        WHERE digitalstate.device_id = %s AND digitalstate.timestamp >= %s AND digitalstate.timestamp <= %s
        ORDER BY digitalstate.timestamp ASC;
        """
        data = (
            digitalstate['device_id'],
            digitalstate['timestamp'] - (odte_config['time_window'] * 1000),
            digitalstate['timestamp']
        )
        cursor.execute(query, data)
        states = cursor.fetchall()
        conn.commit()
        cursor.close()
        conn.close()
        obs = []
        for s in states:
            obs.append({
                'physicalstate_id': s[0],
                'digitalstate_id': s[1],
                'device_id': s[2],
                'physical_timestamp': s[3],
                'digital_timestamp': s[4],
                'elapsed_time': s[4] - s[3]
            })
        availability = get_odte_availability()
        reliability = get_odte_reliability(
            len(obs), 
            odte_config['expected_update_rate'] * odte_config['time_window']
        )
        timeliness = get_odte_timeliness(
            obs, 
            odte_config['desired_timeliness']
        )
        return {
            'availability': availability,
            'reliability': reliability,
            'timeliness': timeliness,
            'value': availability * reliability * timeliness,
        }
    except (Exception, psycopg2.Error) as error:
        raise error

def get_lifecycle(odte, odte_threshold):
    if odte < odte_threshold:
        return 'disentangled'
    else:
        return 'entangled'

def update_digitalstate(digitalstate, db_config):
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
        UPDATE digitalstate
        SET availability = %s, reliability = %s, timeliness = %s, odte = %s, lifecycle = %s
        WHERE id = %s;
        """
        data = (
            digitalstate['availability'],
            digitalstate['reliability'],
            digitalstate['timeliness'],
            digitalstate['odte'],
            digitalstate['lifecycle'],
            digitalstate['id']
        )
        cursor.execute(query, data)
        conn.commit()
        cursor.close()
        conn.close()
    except (Exception, psycopg2.Error) as error:
        raise error

def main():
    try:
        logging.basicConfig(
            level=logging.DEBUG,
            format='%(asctime)s %(levelname)s %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        pendingdigitalstate = request.json
        logging.info('request: ' + str(pendingdigitalstate))
        db_config = get_db_config()
        odte_config = get_odte_config()
        digitalstate = dict(pendingdigitalstate)
        odte = get_odte(digitalstate, db_config, odte_config)
        digitalstate['availability'] = odte['availability']
        digitalstate['reliability'] = odte['reliability']
        digitalstate['timeliness'] = odte['timeliness']
        digitalstate['odte'] = odte['value']
        digitalstate['lifecycle'] = get_lifecycle(
            digitalstate['odte'],
            odte_config['threshold']
        )
        update_digitalstate(digitalstate, db_config)
        logging.info('response: ' + str(digitalstate))
        return json.dumps(digitalstate)
    except Exception as e:
        logging.error(e)
        return json.dumps({'error': str(e)}), 500