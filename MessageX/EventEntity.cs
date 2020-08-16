using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MessageX
{
    public class EventEntity
    {

        public EventEntity(Api api)
        {
            this.api = api;
        }

        private string text = "";
        private Api api;


        /// <summary>
        /// 消息ID
        /// </summary>
        public string Id { set; get; }

        /// <summary>
        /// 消息标题
        /// </summary>
        public string Title { set; get; }

        /// <summary>
        /// 消息封面
        /// </summary>
        public string Cover { set; get; }

        /// <summary>
        /// 消息摘要
        /// </summary>
        public string Abstract { set; get; }

        /// <summary>
        /// 发送消息的人
        /// </summary>
        public UserEntity Sender { set; get; }

        /// <summary>
        /// 消息正文
        /// </summary>
        public string Content
        {
            get
            {
                if (text == "")
                {
                    var body = api.GetBody("/message/getContent", "{\"id\":\"" + Id + "\"}");
                    var value = JsonConvert.DeserializeObject<Dictionary<string, object>>(body);
                    if (value["success"].ToString().ToLower() == "true")
                    {
                        var content = JsonConvert.DeserializeObject<Dictionary<string, object>>(JsonConvert.SerializeObject(value["data"]));
                        this.text = content["content"].ToString();
                    }
                    else
                    {
                        this.text = "获取内容失败";
                    }
                    return text;
                }
                return text;
            }
        }
    }
}
