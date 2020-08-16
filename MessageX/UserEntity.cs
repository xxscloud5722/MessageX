using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MessageX
{
    public class UserEntity
    {
        /// <summary>
        /// Id
        /// </summary>
        public string Id;

        /// <summary>
        /// Token
        /// </summary>
        public string Token;

        /// <summary>
        /// 网名
        /// </summary>
        public string NickName;

        /// <summary>
        /// 头像
        /// </summary>
        public string AvatarUrl;

        /// <summary>
        /// 未读消息数量
        /// </summary>
        public int UnreadMessageCount;
    }
}
