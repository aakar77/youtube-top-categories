# -*- coding: utf-8 -*-

import os
import json
import time
import google.oauth2.credentials
import datetime
import requests
from rfc3339 import rfc3339

import google_auth_oauthlib.flow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from google_auth_oauthlib.flow import InstalledAppFlow


import csv

API_SERVICE_NAME = 'youtube'
API_VERSION = 'v3'
API_KEY = 'AIzaSyBJDLpU7M86r_WgW--1SqZ1x5nOuyXPlI0'


def get_authenticated_service():
    ###flow = InstalledAppFlow.from_client_secrets_file(CLIENT_SECRETS_FILE, SCOPES)
    ###credentials = flow.run_console()
    return build(API_SERVICE_NAME, API_VERSION, developerKey = API_KEY )

def model_video_response(response, category):

    youtubeData = {}
    print("I am in the youtube function")

# Build a resource based on a list of properties given as key-value pairs.
# Leave properties with empty values out of the inserted resource.
def build_resource(properties):
    resource = {}
    for p in properties:
        # Given a key like "snippet.title", split into "snippet" and "title", where
        # "snippet" will be an object and "title" will be a property in that object.
        prop_array = p.split('.')
        ref = resource
        for pa in range(0, len(prop_array)):
            is_array = False
            key = prop_array[pa]

            # For properties that have array values, convert a name like
            # "snippet.tags[]" to snippet.tags, and set a flag to handle
            # the value as an array.
            if key[-2:] == '[]':
                key = key[0:len(key) - 2:]
                is_array = True

            if pa == (len(prop_array) - 1):
                # Leave properties without values out of inserted resource.
                if properties[p]:
                    if is_array:
                        ref[key] = properties[p].split(',')
                    else:
                        ref[key] = properties[p]
            elif key not in ref:
                # For example, the property is "snippet.title", but the resource does
                # not yet have a "snippet" object. Create the snippet object here.
                # Setting "ref = ref[key]" means that in the next time through the
                # "for pa in range ..." loop, we will be setting a property in the
                # resource's "snippet" object.
                ref[key] = {}
                ref = ref[key]
            else:
                # For example, the property is "snippet.description", and the resource
                # already has a "snippet" object.
                ref = ref[key]
    return resource


# Remove keyword arguments that are not set
def remove_empty_kwargs(**kwargs):
    good_kwargs = {}
    if kwargs is not None:
        for key, value in kwargs.items():
            if value:
                good_kwargs[key] = value
    return good_kwargs

def videos_list_by_id(client, **kwargs):
  # See full sample for function
  kwargs = remove_empty_kwargs(**kwargs)

  response = client.videos().list(
    **kwargs
  ).execute()

  return response

if __name__ == '__main__':
    # When running locally, disable OAuthlib's HTTPs verification. When
    # running in production *do not* leave this option enabled.
    os.environ['OAUTHLIB_INSECURE_TRANSPORT'] = '1'
    client = get_authenticated_service()

    #Eror variable
    error = 0

    with open('videoData.csv') as csvfile:
        readCSV = csv.reader(csvfile, delimiter=',')
        for row in readCSV:

            data = {}
            # Making requet for fetching snippet and Tags for a given video
            response=videos_list_by_id(client,part='snippet',id=str(row[0]))
            try:
                for video in response['items']:

                    data['videoId'] = row[0]
                    data['category'] = row[1]
                    data['tags'] = video['snippet']['tags']

                    if video['snippet']['tags'] is  None:
                        error = 1
                    # data['category'] = row[1]
                    # data['tags'] = video['snippet']['tags']
            except Exception as e:
                print(e)
            else:
                # Do not write to new file if error is generated
                if error == 0:
                    # Writting data to outfile
                    try:
                        with open('../Data/videoTags', 'a') as outfile:
                            json.dump(data, outfile)
                            outfile.write('\n')
                    except Exception as e:
                        print('Error in Writting to file')
                        print (e)

                error = 0
