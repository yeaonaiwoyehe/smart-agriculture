// 需要加密的路径
const ENCRYPT_PATHS = [
  '/**' // 所有接口都加密
]

// 不需要加密的路径(优先级高于ENCRYPT_PATHS)
const EXCLUDE_PATHS = [
           "/render",
            "/validate*",
           "/login/**",
           "/login*",
           "/logout",
           "/captchaImage",
           "/register",
           // 一些公共接口
           "/common/**",
           // 文件上传下载等接口
           "/profile/**"  ,
           // 密钥交换接口,请求数据本身就是已经加密过的,不需要再次加密
           "/system/key/**",
           "/getInfo",
           "/getRouters",
           "/captcha*",
           "/signOut*",
           "/screen/**",
           "/system/user/bind",
  // 全局关闭传输加密，与后端 EncryptConfig（EXCLUDE_PATHS 含 /**）对齐：所有接口走明文 JSON
  "/**"
]

/**
 * 路径匹配(支持通配符 **)
 * @param {string} path 实际路径
 * @param {string} pattern 匹配模式
 * @returns {boolean}
 */
function pathMatch(path, pattern) {
  // 确保路径格式一致
  path = path.startsWith('/') ? path : '/' + path;
  pattern = pattern.startsWith('/') ? pattern : '/' + pattern;

  // 特殊处理 /** 模式
  if (pattern === '/**') {
    return true;
  }

  // 转换通配符为正则表达式
  const regexPattern = pattern
    .replace(/\*\*/g, '.*')    // ** 匹配任意字符（包括斜杠）
    .replace(/\*/g, '[^/]+')   // * 匹配除斜杠外的任意字符
    .replace(/\//g, '\\/');    // 转义斜杠

  const regex = new RegExp(`^${regexPattern}$`);
  return regex.test(path);
}

/**
 * 判断请求路径是否需要加密
 * @param {string} path 请求路径
 * @returns {boolean}
 */
export function isEncryptPath(path) {
  // 标准化路径
  path = path.trim();

  // 1. 先检查是否在排除列表中(优先级高)
  const isExcluded = EXCLUDE_PATHS.some(excludePath =>
    pathMatch(path, excludePath)
  );
  if (isExcluded) {
    return false;
  }

  // 2. 再检查是否在加密列表中(优先级低)
  const needsEncryption = ENCRYPT_PATHS.some(encryptPath =>
    pathMatch(path, encryptPath)
  );

  return needsEncryption;
}

// 用于调试的测试函数
function test() {
  const testPaths = [
    '/login',
    '/common/upload',
    '/profile/user',
    '/api/test',
    '/api/test/deep/path',
    '/system/user/list'
  ];

  testPaths.forEach(path => {
    console.log(`Path: ${path}, Needs encryption: ${isEncryptPath(path)}`);
  });
}
