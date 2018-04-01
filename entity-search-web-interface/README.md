# Web Interface For Entity Search and Entity-Semantic Document Search

## Summary
This user interface uses [nodejs](https://nodejs.org/en/) as the server. The backend simply wraps ElasticSearch call. The frontend supports both entity search and entity-semantic document search. Users can click a button to switch between the two.

## How to install
At the folder containing this README:
```
cd backend
npm install
```

```
cd frontend
npm install
```

## How to start
At the folder containing this README:
```cd backend
npm start &
```

* If listen locally (localhost:8080):
```cd frontend
npm run dev
```
* If listen publicly (0.0.0.0:8080):
```cd frontend
npm start &
```

## Troubleshooting
* No error but cannot open the webpage with a browser
  * Clear the cache, or use Incognito mode, or switch to another browser. Will fix this issue soon.
* reror EADDRINUSE:::xxxx means the port xxxx is in use (there is already a running server process)
  * To see if there is node/nodemon process running: 
```aux ps | grep node
```
  * To kill other node processes:
```killall node
```
  * Now you should be able to start servers normally.
