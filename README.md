# flying-saucer

A multisource video downloader for archive.org videos.


## Workflow
First, if the cache to disk flag is set or 
First check to see if the output folder exists. The default is "/tmp", which won't work on non-unix platforms.



Given a valid archive.org download URL, head the resource, require the URL to support range requests.

From the same head request, keep track of the ETag, and use it to break out if any download doesn't satisfy the If-Match clause:
https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.24

Then use the etag to create a folder in the output directory if /tmp exists (checked at boot). This holds all the chunks we need.

Then start downloading, in a standard chunk size of 16KB (chosen because of block sizes on common file systems today). After the download completes, then read all the elements out. 
