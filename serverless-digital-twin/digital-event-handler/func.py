from flask import request
import logging
import json

def main():
    try:
        logging.basicConfig(
            level=logging.DEBUG,
            format='%(asctime)s %(levelname)s %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        digitalstate = request.json
        logging.info('request: ' + str(digitalstate))
        logging.info('response: ' + str(digitalstate))
        return json.dumps(digitalstate)
    except Exception as e:
        logging.error(e)
        return json.dumps({'error': str(e)}), 500