using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace MessageX
{
    public class Api
    {
        private string baseUrl;
        public string Token { get; set; }

        public Api(string baseUrl)
        {
            this.baseUrl = baseUrl;
        }

        public string GetBody(string url, string body = null)
        {
            Console.WriteLine("请求地址: " + url + " 内容: " + body);
            return Send(url, body);
        }

        private string Send(string url, string body = null, int index = 0)
        {
            try
            {
                if (index > 2)
                {
                    return "{}";
                }
                var request = (HttpWebRequest)WebRequest.Create(baseUrl + url);
                request.Method = "POST";
                request.ContentType = "application/json";
                if (Token != null && Token != "")
                {
                    request.Headers.Add("token", Token);
                }

                if (body != null)
                {
                    using (var streamWriter = new StreamWriter(request.GetRequestStream()))
                    {
                        streamWriter.Write(body);
                        streamWriter.Flush();
                        streamWriter.Close();
                    }
                }


                var response = (HttpWebResponse)request.GetResponse();
                if (response.StatusCode != HttpStatusCode.OK)
                {
                    return Send(url, body, index + 1);
                }
                using (var streamReader = new StreamReader(response.GetResponseStream()))
                {
                    return streamReader.ReadToEnd();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
                return Send(url, body, index + 1);
            }
        }
    }
}
