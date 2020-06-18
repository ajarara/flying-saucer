# flying-saucer

A multisource video downloader for archive.org videos.

## Usage
``` sh
ahmads-mbp:flying-saucer ahmadjarara$ ./run.sh --help
Usage: main [OPTIONS]

Options:
  --archive-url TEXT            Mp4 to download from archive.org
  --concurrent-request-max INT  Number of requests to run simultaneously
  --no-cache                    Do not cache chunks on disk in between runs
  -h, --help                    Show this message and exit
```

## Workflow
Require the passed in URL is an Archive.org download URL. Exit otherwise.

Check to see if the passed URL (or the URL it gets redirected to) supports range requests with an ETag. 
If no ETag is returned in the HEAD request, there's no way to tell that previously downloaded chunks are not stale.

From the same head request, keep track of the ETag, and use it to break out if any download doesn't satisfy the If-Match clause:
https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.24

If the disk cache is enabled, use the name to generate a folder in a temporary directory (that survives process reboots).
Use ETags to name chunks on disk in this format: "$etag.$chunkNo"
We get pretty convenient cache invalidation this way: If the ETag changes in between boots we do not use stale chunks.

For this exercise, we do not use the Content-Length header response. 
So we rely on the server telling us our range requests are unsatisfiable to finish, and short circuit.

There may be some requests in flight when we get that response, so we must wait for those to finish.
RxJava's flatMap takes care of this for us, since it completes when the upstream completes and crucially all 
generated downstreams complete. It allows us to specify how many observables are alive at once: 1 is effectively
synchronous, anything greater is parallel.

Then after we stop making network requests, we read chunks off of the repo onto disk. 
