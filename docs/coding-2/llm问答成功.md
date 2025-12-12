(TraeAI-6) D:\项目\Spring\litemall [0:0] $ $token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjEyMyIsImlhdCI6MTc2NTUxMTMzMywiZXhwIjoxNzY1NTk3NzMzfQ.7G0Io5ToJ-FvaACjhnq4sEyARexe8lzp8Om8A2h7e_w"
(TraeAI-6) D:\项目\Spring\litemall [0:0] $ $body = @{                                                    
>>     question = "请帮我查询价格低于100元的商品"                                                        
>>     sessionId = "test-session-$(Get-Date -Format 'yyyyMMddHHmmss')"
>> } | ConvertTo-Json
(TraeAI-6) D:\项目\Spring\litemall [0:0] $ 
(TraeAI-6) D:\项目\Spring\litemall [0:0] $ $headers = @{
>>     "Content-Type" = "application/json"
>>     "Authorization" = "Bearer $token"
>> }
(TraeAI-6) D:\项目\Spring\litemall [0:0] $ 
(TraeAI-6) D:\项目\Spring\litemall [0:0] $ try {
>>     $response = Invoke-RestMethod -Uri "http://localhost:8080/admin/llm/qa" -Method Post -Headers $headers -Body $body
>>     Write-Host "LLM问答服务响应:"
>>     $response | ConvertTo-Json -Depth 10
>> } catch {
>>     Write-Host "错误信息: $($_.Exception.Message)"
>>     Write-Host "响应状态: $($_.Exception.Response.StatusCode)"
>>     Write-Host "响应内容: $($_.Exception.Response.StatusDescription)"
>> }
LLM问答服务响应:
"---\nerrno: 0\ndata:\n  code: 200\n  message: \"success\"\n  answer: \"根据您的查询意图（价格范围查询） ，找到 105 个商品：\\n\\n1. 磨砂杆直杆中性笔 - 价格：¥4.90\\n   高韧笔杆，书写不疲惫\\n\\\n    2. 按动式 三角中油笔 - 价格：¥8.90\\n   进口笔尖，无毒油墨\\n3. 直杆三角中性笔 - 价格：¥9.90\\n   合金笔尖，高强度 笔身\\n4.\\\n    \\ 清新宠物水食钵食盆 - 价格：¥9.90\\n   含银离子的洁净除菌食盆\\n5. 夜间反光防走失宠物 牵引绳 - 价格：¥9.90\\n   编织反光，夜间\\\n    防走失\\n\\n... 还有 100 个商品\"\n  goods: []\n  sessionId: \"test-session-20251212115014\"\n  queryTime: 1765511416654\n  timestamp:\n  - 2025\n  - 12\n  - 12\n  - 11\n  - 50\n  - 16\n  - 654546500\n  fromCache: false\n  queryIntent:\n    queryType: \"price_range\"\n    conditions:\n      max_price: \"100\"\n    sort: \"\"\n    limit: 0\n    confidence: null\n    explanation: null\n    valid: true\nerrmsg: \"成功\"\n"