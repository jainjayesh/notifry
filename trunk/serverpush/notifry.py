#!/usr/bin/env python

# Notifry - Python server push script.
# 
# Copyright 2011 Daniel Foote
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
# This script sends the notification to the backend server for the given source.
# Return codes:
# 0 - Success
# 1 - HTTP error
# 2 - Backend error

import urllib
import urllib2
import sys
import getopt
import json

# Configuration.
BACKEND = 'https://notifrier.appspot.com/notifry';

def usage():
	print "Usage: %s -s <source_key> -t Title -m Message [-u url]" % sys.argv[0]
	print "If message is -, read message from stdin."
	sys.exit()

# Parse our arguments.
optlist, args = getopt.getopt(sys.argv[1:], 's:t:m:u:')

params = {}
params['format'] = 'json'
requiredCount = 0
for key, value in optlist:
	if key == '-s':
		params['source'] = value
		requiredCount += 1
	elif key == '-t':
		params['title'] = value
		requiredCount += 1
	elif key == '-m':
		params['message'] = value
		requiredCount += 1
	elif key == '-u':
		params['url'] = value

# Not enough arguments?
if requiredCount != 3:
	usage()

# Read message from stdin, if required.
if params['message'] == '-':
	params['message'] = sys.stdin.read()

# Prepare our request.
try:
	response = urllib2.urlopen(BACKEND, urllib.urlencode(params))

	# Read the body.
	body = response.read()
	# It's JSON - parse it.
	contents = json.loads(body)

	if contents.has_key('error'):
		print "Server did not accept our message: %s" % contents['error']
		sys.exit(2)
	else:
		print "Message sent OK. Size: %d." % contents['size']

except urllib2.URLError, ex:
	print "Failed to make request to the server: " + str(ex)
	sys.exit(1)
