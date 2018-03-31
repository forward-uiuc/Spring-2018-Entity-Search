# Web Deploy Instruction
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

### Try "localhost:8080/entitylucene" to open the web interface 

# Web Interface Instruction
### In the searchbox, type the format as "#entityCategory enetityContent". The available entityCategories are: 
  - a(application)
  - p(product)
  - s(symptom)
  - f(function)
  - h(hardware)
  - w(general word)
  - k(signal word)

# Server Maintenance Instruction
## erros EADDRINUSE:::xxxx means the port xxxx is in use (there is already a running server process)
### To see if there is node/nodemon process running: 
  - lsof -i:7001
### To kill other node processes:
  - kill -9 pid
### Now you should be able to start servers normally.