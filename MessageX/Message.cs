using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using WebSocketSharp;

namespace MessageX
{
    public class Message
    {
        private WebSocket webSocket;
        private bool close = false;
        private readonly EventCallback callback;
        private readonly string ws;
        private string address = "";
        private string token = "";
        private readonly Api api;
        public delegate void EventCallback(EventEntity eventEntity);

        public Message(string ws, string api, EventCallback callback = null)
        {
            this.callback = callback;
            this.api = new Api(api);
            this.ws = ws;
        }

        /// <summary>
        /// 登录
        /// </summary>
        /// <param name="token"></param>
        public void Login(string token)
        {
            //发送到服务器检查token
            this.api.Token = token;
            var body = api.GetBody("/user/checkToken");
            var result = JsonConvert.DeserializeObject<dynamic>(body);
            if (result["success"] == true)
            {
                this.token = token;
                this.address = this.ws + "/" + token;
            }
            else
            {
                throw new Exception(result["message"]);
            }
        }

        /// <summary>
        /// 获取用户信息
        /// </summary>
        /// <param name="id"></param>
        /// <returns></returns>
        public UserEntity GetUserInfo()
        {
            if (token == "")
            {
                throw new Exception("请先调用登录方法");
            }
            var body = api.GetBody("/user/getUserInfo");
            var result = JsonConvert.DeserializeObject<dynamic>(body);
            if (result["success"] == true)
            {
                return JsonConvert.DeserializeObject<UserEntity>(JsonConvert.SerializeObject(result["data"]));
            }
            return null;
        }


        /// <summary>
        /// 查询消息列表
        /// </summary>
        /// <param name="id"></param>
        /// <returns></returns>
        public List<EventEntity> GetMessageList(string id = null)
        {
            if (token == "")
            {
                throw new Exception("请先调用登录方法");
            }
            var body = api.GetBody("/message/getMessageList", id == null ? "{}" : "{\"id\":\"" + id + "\"}");
            var result = JsonConvert.DeserializeObject<dynamic>(body);
            if (result["success"] == true)
            {
                var values = (List<Dictionary<string, object>>)JsonConvert.DeserializeObject(JsonConvert.SerializeObject(result["data"]), typeof(List<Dictionary<string, object>>));
                var data = new List<EventEntity>();
                values.ForEach(it =>
                {

                    data.Add(new EventEntity(api)
                    {
                        Id = it.Keys.ToList().Contains("id") ? it["id"]?.ToString() : null,
                        Title = it.Keys.ToList().Contains("title") ? it["title"]?.ToString() : null,
                        Cover = it.Keys.ToList().Contains("cover") ? it["cover"]?.ToString() : null,
                        Abstract = it.Keys.ToList().Contains("abstract") ? it["abstract"]?.ToString() : null,
                    });
                });
                return data;
            }
            return null;
        }

        /// <summary>
        /// 建立监听
        /// </summary>
        public void Connection()
        {
            if (token == "")
            {
                throw new Exception("请先调用登录方法");
            }
            webSocket = new WebSocket(address);
            webSocket.OnMessage += (sender, e) =>
            {
                if (e.Data.StartsWith("error"))
                {
                    throw new Exception(e.Data);
                }
                Console.WriteLine("Get Data : " + e.Data);
                var index = e.Data.IndexOf(' ');
                if (index < 0)
                {
                    return;
                }
                var value = JsonConvert.DeserializeObject<Dictionary<string, object>>(JsonConvert.SerializeObject(e.Data.Substring(index + 1)));
                callback?.Invoke(new EventEntity(api)
                {
                    Id = value.Keys.ToList().Contains("id") ? value["id"]?.ToString() : null,
                    Title = value.Keys.ToList().Contains("title") ? value["title"]?.ToString() : null,
                    Cover = value.Keys.ToList().Contains("cover") ? value["cover"]?.ToString() : null,
                    Abstract = value.Keys.ToList().Contains("abstract") ? value["abstract"]?.ToString() : null,
                });
            };

            webSocket.OnOpen += (sender, e) =>
            {
                Console.WriteLine("OnOpen ...");
            };

            webSocket.OnClose += (sender, e) =>
            {
                Console.WriteLine("Close ...");
                if (close)
                {
                    return;
                }
                webSocket.Close();
                Connection();
            };
            webSocket.OnError += (sender, e) =>
            {
                Console.WriteLine("OnError ...");
            };
            webSocket.Connect();
            Console.WriteLine("连接成功 ...");
        }

        /// <summary>
        /// 关闭连接
        /// </summary>
        public void Close()
        {
            if (token == "")
            {
                throw new Exception("请先调用登录方法");
            }
            this.close = true;
            this.webSocket.Close();
        }


        /// <summary>
        /// 读取消息
        /// </summary>
        /// <param name="id"></param>
        public bool ReadMessage(string id)
        {
            if (token == "")
            {
                throw new Exception("请先调用登录方法");
            }
            var body = api.GetBody("/message/readMessage", "{\"id\":\"" + id + "\"}");
            var result = JsonConvert.DeserializeObject<dynamic>(body);
            if (result["success"] == true)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

    }
}
