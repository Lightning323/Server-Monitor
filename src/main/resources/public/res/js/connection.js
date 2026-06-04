const connectionStatusElement = document.getElementById("connectionStatus");
// // --- The Packet Registry ---
const PacketRegistry = {
    handlers: {},
    register(type, callback) {
        this.handlers[type.toLowerCase().trim()] = callback;
        },
    handle(data) {
        let packet;
        try {
            packet = JSON.parse(data);
        } catch (e) {
            console.error("Malformed JSON:", data);
            return;
        }

        if (!packet || !packet.type) {
            console.error("Invalid packet format received (missing type):", packet);
            return;
        }

        try {
            const handler = this.handlers[packet.type.toLowerCase().trim()];
            if (handler) handler(packet.payload);
            else console.warn(`No handler for: ${packet.type}`);
        } catch (e) {
            console.error(`Error in handler for ${packet.type}:`, e);
        }
    },
    send(type, payload) {
        if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({ type, payload }));
        }
    }
};

// --- Your Existing Connection Logic ---
let socket;

function connect() {
    let url = "ws://" + window.location.host + "/ws";
    status("Connecting...");
    socket = new WebSocket(url);

    socket.onopen = () => {
        status("Connected to server");
    };

    socket.onmessage = (event) => {
        // Now everything is routed through the registry
        PacketRegistry.handle(event.data);
    };

    socket.onclose = () => {
        setTimeout(connect, 1000); // Reconnect logic
    };
}

function status(msg){
    console.log(msg);
    connectionStatusElement.innerText = msg;
}

export {connect, PacketRegistry};