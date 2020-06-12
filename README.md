# flying-saucer

A multisource video downloader for archive.org videos.

## Workflow
Given a valid archive.org download URL, head the resource, require the URL to support range requests.

From the Head request, keep track of the ETag, and use it to break out if any download doesn't satisfy the If-Match clause:
https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.24

