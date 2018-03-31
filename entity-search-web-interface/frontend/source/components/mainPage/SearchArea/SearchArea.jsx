import React, { Component } from 'react';
import ReactDOM from 'react-dom'
import _ from 'lodash'
import styles from './SearchArea.scss';
import TextField from 'material-ui/TextField';
import { Button, Icon, Input,Select,Form,Radio,Grid, Modal, Header,Action,Label,Segment, Container,Dropdown} from 'semantic-ui-react'
// import SearchList from './SearchList.jsx' 
import ReactScrollableList from 'react-scrollable-list'
import axios from 'axios'
import IntegrationAutosuggest from './autosuggest.jsx'
import IntegrationAutosuggestDocument from './autosuggestdocument.jsx'

class SearchArea extends Component {
	constructor(props) {
		super(props);
		this.state ={
			searchEntity:'',
			searchConditionForDoc:'',
			cluster:[],
			hits:[],

			isLoading: false,
			value: '',
			allresults: [],
			clusterExamples:[],
			output:[],
			documentName:[],

			open:false,
			addLabels: [],
			active: [],
			activeLabel: [true],
			showDocument:true,
			emptyResult:false,
			categories: [],
			labels:["all",],
			currentLabelarr: [],
			currentEntity:'',
			currentDocNum:'',
			currentText:'',
			PhysicalDoc:'',
			showModal:false,

			//search bar
			searchbyItem:'entity',

			//documentation
			modalOpen: false,


		}
		this.handleResultSelect = this.handleResultSelect.bind(this);
		this.inputChangeHandler = this.inputChangeHandler.bind(this);
		this.inputChangeHandlerForDoc = this.inputChangeHandlerForDoc.bind(this);
		this.resetComponent = this.resetComponent.bind(this);
	
		
		this.baseUrl = 'http://crow.cs.illinois.edu:7001/';
		// this.baseUrl = 'httssp://localhost:7001/';
		this.searchClickHandler = this.searchClickHandler.bind(this);
		this.ClusterHandler = this.ClusterHandler.bind(this);

		this.showDocumentHandler = this.showDocumentHandler.bind(this);
		this.phyDocHandler = this.phyDocHandler.bind(this);

		this.closeModal = this.closeModal.bind(this);
		this.getSearchValue = this.getSearchValue.bind(this);
		this.getSearchDocValue = this.getSearchDocValue.bind(this);

		this.handleMenuItemClick = this.handleMenuItemClick.bind(this);

		this.handleExampleQuery = this.handleExampleQuery.bind(this);
		this.handleExampleQueryForDoc = this.handleExampleQueryForDoc.bind(this);
		this.handleUpdateValue = this.handleUpdateValue.bind(this);
		this.handleUpdateValueForDoc = this.handleUpdateValueForDoc.bind(this);

		this.handleOpen = this.handleOpen.bind(this);
		this.handleClose = this.handleClose.bind(this);


	}
 	
	componentWillMount() {
		document.addEventListener('click', this.handleOutsideClick, false);
		this.resetComponent()
		// console.log(this.props)
	}

	handleUpdateValue(value) {
		this.setState({ searchEntity: value });
	}

	handleUpdateValueForDoc(value) {
		this.setState({ searchConditionForDoc: value });
	}

	handleMenuItemClick(e, { name }){
		this.setState({ searchbyItem: name })
	}

	getSearchValue(value){
		// console.log(value)
		this.setState({
			searchEntity:value
		})

	}

	getSearchDocValue(value){
		// console.log(value)
		this.setState({
			searchConditionForDoc:value
		})

	}



	//show more document results when click the clustercard 
	ClusterHandler(index,name,event){
		this.setState({
			currentEntity: name,
			output:[]
		})
		console.log(`index: ${index}`);
		console.log(`name: ${name}`);
		let currentEntityArr = this.state.cluster[index]
		currentEntityArr = currentEntityArr.document
		console.log("testing",currentEntityArr)
		let hits = this.state.hits;
		let output = [];
		let documentName = [];
		for(let i = 0; i  < currentEntityArr.length; i++){
			for(let j = 0; j < hits.length;j++){
				if(hits[j]._id == currentEntityArr[i].id){
					output.push(hits[j])
							// console.log(hits[j])
												//get the post.txt name
					let value = hits[j]._source.name;
					let fileName = value.substr(0,value.indexOf('.')-0);
					let fileUrl = this.baseUrl+ "getPhysicalDoc/" + value
					axios.get(fileUrl)
						.then((response)=>{
							// console.log(response.data)
							let content = response.data
							let curname = content.substr(0,content.indexOf('<')-0)
							documentName.push(curname)
							this.setState({ documentName: documentName })
						})
						.catch(error =>{
							console.log(error)
						})
				}
				

			}
		}

		//sort the score
		output.sort(function(a,b){
			if (a._score > b._score) {
				return -1;
			} else if (a._score < b._score) {
				return 1;
			}else{
				return 0
			}

		})
			
		// })
		console.log(output)
		this.setState({output: output});

	}

	showDocumentHandler(){
		let result = !this.state.showDocument;
		this.setState({
			showDocument: result
		})
	}

	resetComponent(){
		this.setState({ isLoading: false, output: [], value: '' })
	}

	handleResultSelect(event, { result }){
		// console.log(result)
		this.setState({ 
			value: result.title
		})
	}

	inputChangeHandler(event){
		// console.log(event.target.value)
		this.setState({
			output:[],
			searchEntity:event.target.value,
			activeLabel: [true],


		})

		event.preventDefault();
	}

	inputChangeHandlerForDoc(event){
		// console.log(event.target.value)
		this.setState({
			output:[],
			searchConditionForDoc:event.target.value,
			activeLabel: [true],


		})

		event.preventDefault();
	}

	handleExampleQuery(value,event){
		console.log(value)
		// this.getSearchValue(value)
		this.handleUpdateValue(value);
	}

	handleExampleQueryForDoc(value,event){
		let tmp = value.split(' asdf ')
		// this.getSearchValue(value)
		this.handleUpdateValue(tmp[0]);
		this.handleUpdateValueForDoc(tmp[1]);
	}

	searchClickHandler(event){

		
		let entity = this.state.searchEntity;

		let current_url = this.baseUrl + "search/" + entity;
		if (this.state.searchbyItem !== 'entity') {
			entity = this.state.searchEntity + " " + this.state.searchConditionForDoc
			current_url = this.baseUrl + "esdocumentsearch/" + entity;
		}

		//Qingling Kang's code
		current_url = current_url.split(" ").join("+");
		current_url = current_url.split("#").join("$");

		//Long's code
		console.log(current_url);
		//current_url = current_url.split('').join('');
		//console.log(current_url);
		console.log("Hello")

		axios.get(current_url)
			.then((response)=>{
				console.log("How are you")
				console.log(response.data);
				console.log("cluster");
				let cluster = response.data.clusters;
				let hits = response.data.hits.hits;
				let output = [];
				let documentName =[];
				console.log("test")
				if (cluster.length > 0) {
					for(let i = 0; i  < cluster.length; i++){
						for(let j = 0; j < hits.length;j++){
							if(hits[j]._id == cluster[i].document[0].id){
								output.push(hits[j])

								//get the post.txt name
								//let value = hits[j]._source.name;
								//let fileName = value.substr(0,value.indexOf('.')-0);
								//let fileUrl = this.baseUrl+ "getPhysicalDoc/" + value
								//axios.get(fileUrl)
								//	.then((response)=>{
								//		// console.log(response.data)
								//		let content = response.data
								//		let curname = content.substr(0,content.indexOf('<')-0)
								//		documentName.push(curname)
								//		this.setState({ documentName: documentName })
								//	})
								//	.catch(error =>{
								//		console.log(error)
								//	})
							}
						}
					}
				} else {
					console.log('is it running here?')
					for(let j = 0; j < hits.length;j++){					
						output.push(hits[j])
					}
				}
				console.log("test2")
				
				// console.log("trstint",documentName[0])
				// console.log("output",output)

				// console.log(response.data.hits.hits);
				// console.log(response.data.clusters);


				this.setState({
					currentEntity:'',
					cluster: response.data.clusters,
					hits: response.data.hits.hits,
					output: output,
					clusterExamples:output,
					// documentName:documentName
					
				})

				console.log("fine until")

				console.log("set state is okay")

				if(response.data.hits.total === 0){
					console.log("Get empty result")
					this.setState({
						emptyResult:true
					})
				}

				console.log("set empty is okay")

				// console.log(response.data.clusters);
				// console.log("hits", this.hits);
				// console.log(response.data.hits.hits);
			})
			.catch(error=>{
				console.log(error)

	  		});
	}

	phyDocHandler(value,text,event){

		console.log("value",value)
		let fileName = value.substr(0,value.indexOf('.')-0);
		// console.log(fileName)
		console.log("text",text)

		this.setState({
			currentDocNum:fileName,
			currentText:text
		})
		let fileUrl = this.baseUrl+ "getPhysicalDoc/" + value
		axios.get(fileUrl)
			.then((response)=>{
				console.log(response.data)
				let content = response.data
				
				this.setState({
					physicalDoc:response.data,
					showModal:true
				})
			})
			.catch(error =>{
				console.log(error)
			})
	}
	closeModal(){
		this.setState({
			physicalDoc: '',
			currentDocNum:'',
			showModal:false
		})

	}

	handleOpen(){
		this.setState({ modalOpen: true })
	}

 	handleClose(){
 		this.setState({ modalOpen: false })
 	}

   	render(){
   		const { 
   			isLoading,
   			value, 
   			output,
   			addLabels,
   			labels,
   			cluster,
   			hits,
   			currentEntity,
   			showDocument,
   			showModal,
   			currentDocNum,
   			physicalDoc,
   			currentText,
			clusterExamples,
   			searchbyItem,
   			emptyResult,
   			documentName
   		} = this.state
   		// console.log("tsting",documentName[0])
   		// console.log("testing",output);
   		// console.log(this.state.results.length)
   		// console.log(this.state.searchEntity)
   		// if(output.length == 0){
   		// 	output[0] = "No result found yet!"
   		// };

   		let phyDoc=(
			<Modal id="modal"  open={this.state.showModal}>
			
			<Header content={currentDocNum} />

			    <Modal.Content scrolling>
					<div>
						{physicalDoc}
					</div>
					 </Modal.Content>
			    <Modal.Actions>
			     <Button color='green' onClick = {this.closeModal}>
			        <Icon name='remove'/> Close
			      </Button>
			    </Modal.Actions>

		 </Modal>

   		)	

   		let EntityResult =(
   			<div className="resultcard1">
				
				 	<div className="header resultheader">
				 		<Header as='h2'>Found {cluster.length} entities</Header>
				 	</div>
				 	<div className="content entityContent">
				 	{cluster.map((item,i)=> 
					 	<div key={i} className="ui card clustercard entityhover">
							<div className="content">
					 			<div className="entityHeader">{item.name}</div>	
							</div>
							<div className="content">
							    {
							    /*
							    <div className="description resultText">
	 								<span>{clusterExamples[i]._source.text.substr(0,clusterExamples[i]._source.charOffset)}</span>
	 							 	<span className="highlight">{clusterExamples[i]._source.text.substr(clusterExamples[i]._source.charOffset,clusterExamples[i]._source.name.length)}</span>
	 							 	<span>{clusterExamples[i]._source.text.substr(parseInt(clusterExamples[i]._source.charOffset)+parseInt(clusterExamples[i]._source.name.length))}</span>
	 							 	<span> <a onClick={this.phyDocHandler.bind(this,clusterExamples[i]._source.name,clusterExamples[i]._source.text)}>read more...</a> </span>
	 							</div>
							    */
							    }
								<div className="description resultText">

									<div> <a href={clusterExamples[i]._source.url}>
							    	{clusterExamples[i]._source.url} </a> </div>
							    	<div> {clusterExamples[i]._source.title} </div>
	 								<span>{clusterExamples[i]._source.text.substr(0,200)}</span>
	 							</div>
								<div className="meta">score:&nbsp;{item.score} (from {item.document.length} relevant web pages) </div> 					 			
 							</div>
	 						
						</div>
					)}
				</div>
			</div>
		)

   		let DocumentResult=(
			
   					<div className="docblock">
				 	{output.map((item,i)=> 
					 	<div key={i} className="ui card padded clustercard raised">
							<div className="content">
							
								<div className="ui top left attached label"><a href={item._source.url}>{item._source.url}</a></div>
								
								<div className="header docheader">
									{item._source.title}
								</div>
								<div className="description resultText">
									{/*
									<span>{item._source.text.substr(0,item._source.charOffset)}</span>
	 							 	<span className="highlight">{item._source.text.substr(item._source.charOffset,item._source.name.length)}</span>
	 							 	<span>{item._source.text.substr(parseInt(item._source.charOffset)+parseInt(item._source.name.length))}</span>
	 								<span><a onClick={this.phyDocHandler.bind(this,item._source.name,item._source.text)}> read more..</a></span>
									*/}
	 								<span>{item._source.text.substr(0,200)}</span>
	 							</div>
								<div className="meta">score:&nbsp;{item._score}</div>
							</div>
						</div>
					)}
				 	{phyDoc}
				 	</div>
   		)

   		let searchBar = (
   			<Container className="searchbar">
				<Grid columns='equal'>
					
		     		{(searchbyItem === 'entity')?
		     		(
		     		<Grid.Column width={9}>
		     			<div> Specify entities and describe their contexts by keywords: </div>
		     			<IntegrationAutosuggest getSearchValue={this.getSearchValue} onEnter={this.searchClickHandler} onUpdateValue={this.handleUpdateValue} searchValue={this.state.searchEntity} />
		     		</Grid.Column>
		     		):
		     		(
		     		<Grid.Column width={9}>
		     			<div> Describe page type: </div>
		     			<IntegrationAutosuggest getSearchValue={this.getSearchValue} onEnter={this.searchClickHandler} onUpdateValue={this.handleUpdateValue} searchValue={this.state.searchEntity} />
		     		
		     			<div> Describe semantic conditions: </div>
		     			<IntegrationAutosuggestDocument getSearchValue={this.getSearchDocValue} onEnter={this.searchClickHandler} onUpdateValue={this.handleUpdateValueForDoc} searchValue={this.state.searchConditionForDoc} />
		     		</Grid.Column>
		     		)}
		     		

					<Grid.Column width={7}>
						<button className="ui icon button" role="button" onClick={this.searchClickHandler}>
							<i aria-hidden="true" className="search icon"></i>
							Search
						</button>
						<Dropdown floating labeled button className='icon' text='Choose Search Type'>
						    <Dropdown.Menu>
							    <Dropdown.Item name='entity' active={searchbyItem === 'entity'} onClick={this.handleMenuItemClick} >
							    	Entity Search
							    </Dropdown.Item>
							    <Dropdown.Item name='doc' active={searchbyItem  === 'doc'} onClick={this.handleMenuItemClick} >
							    	Entity-semantic Document Search
							    </Dropdown.Item>
						    </Dropdown.Menu>
						</Dropdown>	
						{/*showDocument ?
							(<Button className="clusterbutton" onClick={this.showDocumentHandler} disabled={searchbyItem === 'doc'}>
								Hide Document
 							</Button>)
 							:
 							(<Button className="clusterbutton" onClick={this.showDocumentHandler} disabled={searchbyItem === 'doc'}>
								Show Document
 							</Button>)*/
							}	
					</Grid.Column>
				</Grid>
			</Container>
   		)

   		let suggestInput = (searchbyItem === 'entity')?(

   			<Container className="searchbar">
   					<Header size='small'>Example Queries for Entity Search</Header>
   				  	<Grid columns='equal'>
					    <Grid.Row className="example">
					      	<Grid.Column>
					       		<Header as='h4' 
					       				color='red'
					       				onClick={this.handleExampleQuery.bind(this,"#professor mining")}>
					       				#professor mining
					       		</Header> 
					     	</Grid.Column>
					      	<Grid.Column>
								<Header as='h4' color='orange' 
										onClick={this.handleExampleQuery.bind(this,"#professor #topic mining")}>
										#professor #topic mining
								</Header>  						       		
					      	</Grid.Column>
					      	<Grid.Column>
					       		<Header as='h4' 
					       				color='olive'
					       				onClick={this.handleExampleQuery.bind(this,"reset through #topic machine learning")}
					       				>
					       				#topic machine learning
					       		</Header>
					      	</Grid.Column>
					    </Grid.Row>

					    <Grid.Row>
						      <Grid.Column width={5}>
						      	 	<Header as='h4' 
						      	 	 		color='teal'
						      	 	 		onClick={this.handleExampleQuery.bind(this,"	#professor #sponsor_agency")}>
						      	 	 		#professor #sponsor_agency
						      	 	</Header>
						      </Grid.Column>
						      <Grid.Column width={6}>
						       		<Header as='h4' 
						       				color='blue'
						       				onClick={this.handleExampleQuery.bind(this,"#conference #professor")}
						       				>
						       				#conference #professor
						       		</Header>
						      </Grid.Column>						      		
						</Grid.Row>
					</Grid>
   			</Container>
   		)
   		:
   		(
   		<Container className="searchbar">
   					<Header size='small'>Example Queries for Entity Semantic Document Search</Header>
   				  	<Grid columns='equal'>
					    <Grid.Row className="example">
					      	<Grid.Column>
					       		<Header as='h4' 
					       				color='red'
					       				onClick={this.handleExampleQueryForDoc.bind(this,"@near ( #professor #phone #topic ) asdf @near ( interests vision )")}>
					       				Search for professor home pages with interest in computer vision
					       		</Header> 
					     	</Grid.Column>
					    </Grid.Row>
						<Grid.Row>
					      	<Grid.Column>
								<Header as='h4' color='orange' 
										onClick={this.handleExampleQueryForDoc.bind(this,"@near ( #professor #phone #topic ) asdf @contains ( biology )")}>
										Search for CS professor home pages with work related to biology domain
								</Header>  						       		
					      	</Grid.Column>
					    </Grid.Row>
					    <Grid.Row>
						      <Grid.Column>
						      	 	<Header as='h4' 
						      	 	 		color='teal'
						      	 	 		onClick={this.handleExampleQueryForDoc.bind(this,"@contains ( hours #professor ) asdf @contains ( mining )")}>
						      	 	 		Search for CS courses related to data mining
						      	 	</Header>
						      </Grid.Column>
						</Grid.Row>
						<Grid.Row>
						      <Grid.Column>
						       		<Header as='h4' 
						       				color='blue'
						       				onClick={this.handleExampleQueryForDoc.bind(this,"@contains ( exam #professor ) @nears ( #number hours ) asdf @contains ( language processing )")}
						       				>
						       				Search for CS courses related to Language Processing
						       		</Header>
						      </Grid.Column>						      		
						</Grid.Row>
					</Grid>
   			</Container>
   		)
   		let documentation = (
   			<Container className="searchbar">
			   	<Modal
        			trigger={<a onClick={this.handleOpen}>Documentation</a>}
			        open={this.state.modalOpen}
			        onClose={this.handleClose}
			        basic
			        size='small'
      			>
        <Modal.Content>
          <img style={{width:'100%'}} src="./assets/documentation.png"/>
        </Modal.Content>
        <Modal.Actions>
          <Button color='green' onClick={this.handleClose} inverted>
            <Icon name='checkmark' /> Got it
          </Button>
        </Modal.Actions>
      </Modal>
   			</Container>
   		)

		return(
			<div className = "SearchArea">
				{searchBar}
				{suggestInput}
				{documentation}
				{
					hits.length != 0 ? 
					    ( 
					    	searchbyItem === 'entity' ?
								<Container className ="searchResult">
									<Grid columns={showDocument? 2 :1 }>						
							 			<Grid.Column width={showDocument? 10 :16}>
							 				{EntityResult}
							 			</Grid.Column>
							 			{
								 			showDocument?
									 			(
										 			<Grid.Column width={6} className="correspondingDoc">
										 				<div className="ui card resultcard">
															<div className="content ">
				 												<div className="header resultheader">
				 													<Header as='h3'>Relevant Web Pages</Header>
				 												</div>

										 							{DocumentResult}
										 					</div>
										 				</div>
										 			</Grid.Column>
									 			)
								 			:null
							 			}
							 		</Grid>
								</Container>
								:
								<Grid.Column width={16}>
									<div className="ui card resultcard">
										<div className="content ">
				 							<div className="header resultheader">
				 								<Header as='h2'>Found {output.length} relevant documents</Header>
				 							</div>
				 							 {DocumentResult}
				 						</div>
				 					</div>	
								</Grid.Column>
						)
					: 
					(
						emptyResult?
						(
							<Container className ="searchResult">
								<Grid columns={1}>						
						 			<Grid.Column width={16}>
						 				<div className="ui card resultcard1">
						 					<div className="content">
						 					 	<Header as='h3'>No results containing all your search terms were found.</Header>
						 					 </div>
						 				</div>
						 			</Grid.Column>
							 	</Grid>
							</Container>
						) :null 
					)

				}
			</div>
		)
	
  	}
}
export default SearchArea;



