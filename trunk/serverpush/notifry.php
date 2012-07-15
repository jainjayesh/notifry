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
 * Return codes:
 * 0 - Success
 * 1 - An error occurred.
 */

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

/**
 * Notifry someone.
 * @param string $source The source key.
 * @param string $title The title of the notification.
 * @param string $message The message body of the notification.
 * @param string|NULL $url The URL to send along with it.
 * @param string $backend The backend URL.
 * @return array An array with a boolean key 'success'. On false, another
 * key is set with 'error', on true, a key is set with 'message'.
 */
function notifry($source, $title, $message, $url = NULL, $backend = 'https://notifrier.appspot.com/notifry')
{
	$params = array();
	$params['source'] = $source;
	$params['message'] = $message;
	$params['title'] = $title;
	$params['format'] = 'json';
	if( false === is_null($url) )
	{
		$params['url'] = $url;
	}
	
	$encodedParameters = array();
	foreach( $params as $key => $value )
	{
		$encodedParameters[] = $key . "=" . urlencode($value);
	}
	$body = implode("&", $encodedParameters);

	// Using CURL, send the request to the server.
	$c = curl_init($backend);
	curl_setopt($c, CURLOPT_POST, true);
	curl_setopt($c, CURLOPT_POSTFIELDS, $body);
	curl_setopt($c, CURLOPT_RETURNTRANSFER, true);
	curl_setopt($c, CURLOPT_CONNECTTIMEOUT, 20);
	$page = curl_exec($c);

	// Parse the result.
	$result = array('success' => false);
	if( $page !== FALSE )
	{
		// The result is JSON encoded.
		$decoded = json_decode($page, TRUE);
		if( $decoded === FALSE )
		{
			$result['error'] = "Failed to decode server response: " . $page;
		}
		else
		{
			if( isset($decoded['error']) )
			{
				$result['error'] = $decoded['error'];
			}
			else
			{
				$result['success'] = true;
				$result['message'] = "Success! Message size " . $decoded['size'];
			}
		}
	}
	else
	{
		$result['error'] = curl_error($c);
	}

	curl_close($c);

	return $result;
}

// Prepare our parameters.
$message = $options['m'];

if( $message == '-' )
{
	$message = file_get_contents('php://stdin');
}

$url = null;

if( isset($options['u']) )
{
	$url = $options['u'];
}

$result = notifry($options['s'], $options['t'], $message, $url);

if( $result['success'] == FALSE )
{
	echo $result['error'], "\n";
	exit(1);
}
else
{
	echo $result['message'], "\n";
	exit(0);
}

?>
