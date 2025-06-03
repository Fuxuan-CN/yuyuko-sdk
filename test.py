
import asyncio
import websockets

async def echo(websocket):
    async for message in websocket:
        print(f"Received message: {message}")
        await websocket.send(message)  # 将收到的消息原样返回给客户端

async def main():
    async with websockets.serve(echo, "localhost", 6789):
        print("WebSocket server started on ws://localhost:6789")
        await asyncio.Future()  # 运行直到手动停止

if __name__ == "__main__":
    asyncio.run(main())