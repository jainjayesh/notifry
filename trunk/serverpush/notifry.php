#!/usr/bin/env php
<?
/**
 * Notifry - PHP server push script.
 * 
 * Copyright 2011 Daniel Foote
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * This script sends the notification to the backend server for the given source.
 */

// Configuration.
$BACKEND = 'https://notifrytest.appspot.com/notifry';

// Parse the command line arguments.
$options = getopt("s:t:m:u:");

function usage()
{
	echo "Usage: ", $_SERVER['argv'][0], " -s <source_key> -t Title -m Message [-u url]\n";
	echo "If messages is -, read the message from stdin.\n";
	exit();
}

if( $options == FALSE )
{
	// Bad options.
	usage();
}
if( !isset($options['s']) || !isset($options['m']) || !isset($options['t']) )
{
	// Missing parameters.
	usage();
}

// Prepare our parameters.
$params = array();
$params['source'] = $options['s'];
$params['message'] = $options['m'];

if( $params['message'] == '-' )
{
	$params['message'] = file_get_contents('php://stdin');
}

$params['title'] = $options['t'];
$params['format'] = 'json';

if( isset($options['u']) )
{
	$params['url'] = $options['u'];
}

// Encode the parameters for transport.
$encodedParameters = array();
foreach( $params as $key => $value )
{
	$encodedParameters[] = $key . "=" . urlencode($value);
}
$body = implode("&", $encodedParameters);

// Using CURL, send the request to the server.
$c = curl_init($BACKEND);
curl_setopt($c, CURLOPT_POST, true);
curl_setopt($c, CURLOPT_POSTFIELDS, $body);
curl_setopt($c, CURLOPT_RETURNTRANSFER, true);
curl_setopt($c, CURLOPT_CONNECTTIMEOUT, 20);
$page = curl_exec($c);

// Parse the result.
if( $page !== FALSE )
{
	// The result is JSON encoded.
	$decoded = json_decode($page, TRUE);
	if( $decoded === FALSE )
	{
		echo "Failed to decode server response: ", $page, "\n";
	}
	else
	{
		if( isset($decoded['error']) )
		{
			echo "Server did not accept our message: ", $decoded['error'], "\n";
		}
		else
		{
			echo "Success! Message size ", $decoded['size'], ".\n";
		}
	}
}
else
{
	echo "HTTP error: ", curl_error($c), "\n";
}

curl_close($c);

?>
