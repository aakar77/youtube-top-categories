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

#Kafka imports
from kafka import KafkaProducer
from kafka.errors import KafkaError

producer = KafkaProducer(bootstrap_servers= ['localhost:9092'])

# The CLIENT_SECRETS_FILE variable specifies the name of a file that contains
# the OAuth 2.0 information for this application, including its client_id and
# client_secret.

###CLIENT_SECRETS_FILE = "client_secret.json"

# This OAuth 2.0 access scope allows for full read/write access to the
# authenticated user's account and requires requests to use an SSL connection.

###SCOPES = ['https://www.googleapis.com/auth/youtube.force-ssl']

API_SERVICE_NAME = 'youtube'
API_VERSION = 'v3'
API_KEY = 'AIzaSyBJDLpU7M86r_WgW--1SqZ1x5nOuyXPlI0'


def get_authenticated_service():
    ###flow = InstalledAppFlow.from_client_secrets_file(CLIENT_SECRETS_FILE, SCOPES)
    ###credentials = flow.run_console()
    return build(API_SERVICE_NAME, API_VERSION, developerKey = API_KEY )

def model_video_response(response, category):

    data = {}
    d={}
    if response == None:
        print("No data for")
    else:

        for video in response['items']:

            #print("--------------------\n video \n -------------------------")
            if 'tags' in video['snippet']:
                print('Yes')
            else:
                print('No')

            data['videoId'] = video['id']['videoId']

            data['publishedAt'] = video['snippet']['publishedAt']

            data['channelId'] = video['snippet']['channelId']

            data['title'] = video['snippet']['title']

            data['description'] = video['snippet']['description']

            data['url'] = video['snippet']['thumbnails']['default']['url']

            data['channelTitle'] = video['snippet']['channelTitle']

            data['category'] = category

            # Making requet for fetching snippet and statistics data for the given video
            response1=videos_list_by_id(client,part='snippet,statistics',id=data['videoId'])

           # print(response1)
           # Iterating over the response1 list
            try :
                for v in response1['items']:
                    # Parsing the video statistics and dumping them to MemSQL folder
                    d['viewCount']=v['statistics']['viewCount']
                    d['likeCount'] = v['statistics']['likeCount']
                    d['dislikeCount'] = v['statistics']['dislikeCount']
                    d['favoriteCount'] = v['statistics']['favoriteCount']
                    d['commentCount'] = v['statistics']['commentCount']
                    d['videoId'] = data['videoId']
                    json_d = json.dumps(d)

                    # Constructing JSON object for tags
                    try:
                        tags_object = {'videoId': data['videoId'], 'category' : data['category'], 'tags' : v['snippet']['tags'] }
                    except Exception as e:
                        print("Error is constructing object")

                    # Writting JSON object into the VideoTags file
                    try:
                        with open('/home/inspire/Desktop/videoTags', 'a') as outfile:
                            json.dump(tags_object, outfile)
                    except Exception as e:
                        print (e)
                    #print(json_d)
            except Exception as e:
                print "Error"
            else:
                #print "Success for Video ID"+d['videoId']
                producer.send('statistics_topic', json.dumps(d))
                a = 2
            print str(data) + "\n"
            producer.send('videos_topic', json.dumps(data))

        '''
        Not to be disturbed
            #producer = KafkaProducer(value_serializer = lambda v:json.dumps(v).encode('utf-8'))
        '''




def model_category_response(response):
    data = {}
    categories = []


    for c in response['items']:
        categories.append(c['snippet']['title'])

    return categories



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



def video_categories_list(client, **kwargs):
    # See full sample for function
    kwargs = remove_empty_kwargs(**kwargs)

    response = client.videoCategories().list(
        **kwargs
    ).execute()

    return model_category_response(response)


def search_list_by_keyword(client, **kwargs):
    # See full sample for function
    kwargs = remove_empty_kwargs(**kwargs)
    category = kwargs['q']
    #print("testing for: {}".format(cat))
    response = client.search().list(
        **kwargs
    ).execute()

    return model_video_response(response,category)

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

    categories = video_categories_list(client,
                          part='snippet',
                          regionCode='US')


    mydatetime = datetime.datetime.now() - datetime.timedelta(days=1*365)
    days = 0

    while True:

        days = days + 1

        mydatetime1 = mydatetime + datetime.timedelta(days=days)
        mydatetime2 = mydatetime + datetime.timedelta(days=days+1)

        #2017-07-16T19:20:30-03:00
        #2013-12-29T19:09:07.630167

        print(mydatetime)
        print(mydatetime.isoformat('T'))

        for category in categories:

            #nine_hours_from_now = datetime.now().isoformat() + timedelta(hours=9)

            #print("The category is {}".format(category))
            search_list_by_keyword(client,
                                  part='snippet',
                                  maxResults=50,
                                  q=category,
                                  publishedAfter = str(rfc3339(mydatetime1)),
                                  publishedBefore = str(rfc3339(mydatetime2)),
                                  type='video')

            time.sleep(1)
