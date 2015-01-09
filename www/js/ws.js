var server = {
	debug: true,
	isConnected : false,
	checkServerOffline : false,
	isServerOffline : false,
	reconnectInterval : 1000,
	quickReconnect : 500,
	slowReconnect : 1000,
	queue : [],
	connect : function() {
		console.log("Connecting WebSocket");
		var location;
		if (document.location.protocol == "http:") {
			location = "ws://";
		} else {
			location = "wss://";
		}
		location += document.location.host + "/ws";
		this.socket = new WebSocket(location);
		this.socket.server = this;
		this.socket.onopen = this.onOpen;
		this.socket.onmessage = this.onMessage;
		this.socket.onclose = this.onClose;
		this.socket.onerror = this.onError;
	},

	onOpen : function() {
		// send pending messages
		while (this.server.queue.length != 0) {
			var msg = this.server.queue.shift();
			this.server.sendRaw(msg);
		}

		if (this.server.isServerOffline) {
			if (this.server.debug) {
				location.reload();
			}
			console.log(new Date() + " Reconnected to websocket");
			tempAlert("Reconnected to the server");
		} else {
			console.log(new Date() + " Connected to websocket");
		}
		this.server.isConnected = true;
		this.server.checkServerOffline = false;
		this.server.isServerOffline = false;
		this.server.reconnectInterval = this.server.quickReconnect;
	},

	onMessage : function(message) {
		var json = JSON.parse(message.data);
		console.log("Received: " + JSON.stringify(json));
		$("." + json.service).trigger("message", json);
	},
	onClose : function() {
		if (this.server.isConnected) {
			console.log("WebSocket disconnected " + new Date());
		} else {
			console.log("WebSocket failed to connect");
		}
		this.server.isConnected  = false;
		if (this.server.checkServerOffline == true) {
			if (this.server.isServerOffline == false) {
				console.log("Server is offline");
				tempAlert("It looks like you have been disconnected.");
				// avoid overhead when server is known to be offline
				this.server.reconnectInterval = this.server.slowReconnect;			
			}
			this.server.isServerOffline = true;
		}
		this.server.checkServerOffline = true;
		var self = this;
		this.socket = null;
		setTimeout(function() {
			self.server.connect();
		}, self.server.reconnectInterval);
	},

	onError: function onError(event) {
		console.log("Error connecting websocket");
	},

	send : function(json) {
		this.sendRaw(JSON.stringify(json));
	},

	sendRaw : function(message) {
		console.log("Sending: " + message);
		if (this.socket && this.socket.readyState == 1) {
			this.socket.send(message);
		} else {
			this.queue.push(message);
		}
	}
};

function tempAlert(msg){
	$("div.tmpalert").remove();
	var el = document.createElement("div");
	el.setAttribute("class", "tmpalert");
	el.setAttribute("style","padding:30px;position:absolute;top:10px;left:40%;background-color:#ccc;z-index:300000");
	el.innerHTML = msg;
	$(el).on("click", function() {
		this.remove();
	});
	document.body.appendChild(el);
}