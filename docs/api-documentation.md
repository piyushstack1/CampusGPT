# API Documentation

`POST /api/chat/ask`

```json
{"question":"Explain normalization","sessionId":1,"documentIds":[]}
```

Response:

```json
{"answer":"...","sources":[{"documentId":1,"filename":"dbms.pdf","pageNumber":4,"chunkId":"1-p4-c1","excerpt":"..."}],"grounded":true,"sessionId":1}
```

Upload uses multipart form field `file`. Errors return `{code,message,timestamp}`.
