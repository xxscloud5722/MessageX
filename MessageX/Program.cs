using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MessageX
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("开始连接");

            //创建消息对象
            var m = new Message("ws://ms.xxscloud.com/api", "http://ms.xxscloud.com/api", (s) =>
            {
                Console.WriteLine("有消息来了");
            });


            //登录
            m.Login("token_123");

            //获取用户信息
            m.GetUserInfo();

            //获取消息列表， 传入游标或者不传
            var data = m.GetMessageList();

            //这里才是开始监听服务器推送的消息
            m.Connection();

            Console.WriteLine(data[0].Content);
            Console.ReadKey();
        }
    }
}
