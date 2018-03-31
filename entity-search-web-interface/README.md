# Web Deploy Instruction \[UPADATING THIS README...\]
## How to install
### cd backend
  - npm install

### cd frontend
  - npm install

## How to start
### cd backend
  - npm start &

### cd frontend
#### If listen locally (localhost:8080):
  - npm run dev
#### If listen publicly (0.0.0.0:8080):
  - npm start &

# Server Maintenance Instruction
## erros EADDRINUSE:::xxxx means the port xxxx is in use (there is already a running server process)
### To see if there is node/nodemon process running: 
  - aux ps | grep node
### To kill other node processes:
  - killall node
### Now you should be able to start servers normally.
