"""
it subscribes to a topic
this topic is published by an mqtt broker

the received data becomes the body of an http request
"""

import argparse
import logging
import json
import paho.mqtt.client as mqtt
import requests

def set_log():
    """
    set logging
    """
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s %(levelname)s %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )

def check_args():
    """
    check required command line args:
    - mqtt broker url
    - topic(s) to subscribe to
    - fission router url
    """
    parser = argparse.ArgumentParser(description='mqtt-connector')
    parser.add_argument('--broker-url', type=str, required=True)
    parser.add_argument('--topic', type=str, nargs='+', required=True)
    parser.add_argument('--router-url', type=str, required=True)
    args = parser.parse_args()
    logging.info(args)
    return args

def on_connect(client, userdata, flags, rc):
    """
    callback for mqtt client on connection
    """
    logging.info("connected with result code " + str(rc))
    for topic in userdata['topics']:
        client.subscribe(topic)
        logging.info("subscribed to " + topic)

def sendtofn(url, payload):
    """
    send http request to fission router
    """
    response = requests.post(
        url,
        json=payload
    )
    if response.status_code == 200:
        return response.json()
    else:
        raise Exception('error: ' + str(response.status_code))

def on_message(client, userdata, msg):
    """
    callback for mqtt client on message
    """
    logging.info(msg.topic + " " + str(msg.payload))
    try:
        physicalevent = json.loads(msg.payload)
        physicalevent['device_id'] = msg.topic.split('/')[1]
        physicalstate = sendtofn(
            'http://' + userdata['router_url'] + '/physicalevents', 
            physicalevent
        )
        logging.info(physicalstate)
        pendingdigitalstate = sendtofn(
            'http://' + userdata['router_url'] + '/physicalstates',
            physicalstate
        )
        logging.info(pendingdigitalstate)
        digitalstate = sendtofn(
            'http://' + userdata['router_url'] + '/pendingdigitalstates',
            pendingdigitalstate
        )
        logging.info(digitalstate)
        digitalevent = sendtofn(
            'http://' + userdata['router_url'] + '/digitalstates',
            digitalstate
        )
        logging.info(digitalevent)
    except Exception as e:
        logging.error(e)

def connect_to_broker(args):
    """
    connect to mqtt broker
    """
    client = mqtt.Client(
        userdata={
            'topics': args.topic,
            'router_url': args.router_url
        }
    )
    client.on_connect = on_connect
    client.on_message = on_message
    broker_host = args.broker_url.split(':')[0]
    broker_port = int(args.broker_url.split(':')[1])
    client.connect(broker_host, broker_port, 60)
    client.loop_forever()
    
def main():
    set_log()
    args = check_args()
    connect_to_broker(args)

if __name__ == '__main__':
    main()