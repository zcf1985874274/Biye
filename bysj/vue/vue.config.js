module.exports = {
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    host: '0.0.0.0',  // 推荐添加：允许外部网络访问（局域网或公网穿透）
    port: 8081,       // 如果你的前端是 8081，明确指定（可选）
    allowedHosts: [
      '.natappfree.cc',  // 允许所有 Natapp 免费域名
      'localhost',
      '127.0.0.1'
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/api': ''
        }
      }
    }
  }
}