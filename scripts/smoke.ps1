param(
    [string]$BaseUrl = 'http://127.0.0.1:18080',
    [string]$AdminUsername = 'admin',
    [string]$AdminPassword = $env:MALL_ADMIN_PASSWORD,
    [string]$DevKey = $env:MALL_SIMULATION_KEY,
    [int]$TimeoutSeconds = 120
)
$ErrorActionPreference = 'Stop'
$base = [Uri]$BaseUrl
if (-not $base.IsLoopback -or $base.Scheme -notin @('http','https') -or $base.AbsolutePath -ne '/' -or $base.UserInfo -or $base.Query -or $base.Fragment) { throw '演示脚本仅允许本机 HTTP 根地址' }
if (-not $AdminPassword -or -not $DevKey) { throw '请先导入 MALL_ADMIN_PASSWORD 和 MALL_SIMULATION_KEY' }
$BaseUrl = $BaseUrl.TrimEnd('/')
$run = [Guid]::NewGuid().ToString('N').Substring(0,20)
$script:orderNo = $null
function Api([string]$Method, [string]$Path, $Body = $null, [hashtable]$Headers = @{}) {
    $request = @{ Method=$Method; Uri="$BaseUrl$Path"; Headers=$Headers; TimeoutSec=15; MaximumRedirection=0 }
    if ($null -ne $Body) { $request.ContentType='application/json; charset=utf-8'; $request.Body=[Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 15 -Compress)) }
    try { Invoke-RestMethod @request }
    catch { throw "请求失败：$Method $Path。请检查服务日志和对应业务号；为保护凭证，不输出请求内容。" }
}
function Until([string]$Description, [scriptblock]$Read, [scriptblock]$Matches) {
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $value = & $Read
        if (& $Matches $value) { return $value }
        Start-Sleep -Seconds 1
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "$Description 超时。订单号：$script:orderNo；请检查 /admin/v1/tasks。"
}
function Assert-Equal($Actual, $Expected, [string]$Description) {
    if ($Actual -ne $Expected) { throw "$Description 不符合预期：实际 $Actual，预期 $Expected" }
}
try {
    $admin = Api POST '/admin/v1/auth/login' @{username=$AdminUsername;password=$AdminPassword}
    $adminHeaders = @{Authorization="Bearer $($admin.token)"}
    $credentials = @{username="smoke_$run";password=([Guid]::NewGuid().ToString('N'))}
    $null = Api POST '/api/v1/auth/register' $credentials
    $buyer = Api POST '/api/v1/auth/login' $credentials
    $memberHeaders = @{Authorization="Bearer $($buyer.token)"}
    $devHeaders = @{'X-Dev-Key'=$DevKey}
    $address = Api POST '/api/v1/addresses' @{receiver='演示买家';phone='13800000000';province='浙江';city='杭州';district='西湖';detail='本地测试地址';defaultAddress=$true} $memberHeaders
    $category = Api POST '/admin/v1/catalog/categories' @{name="演示分类_$run"} $adminHeaders
    $product = Api POST '/admin/v1/products' @{categoryId=$category.id;title="演示笔记本_$run";imageUrls=@();skus=@(@{specificationValueIds=@();priceFen=1990})} $adminHeaders
    $detail = Api GET "/admin/v1/products/$($product.id)" $null $adminHeaders
    $sku = $detail.skus[0].id
    $stockHeaders = $adminHeaders.Clone(); $stockHeaders['Idempotency-Key']="stock_$run"
    $null = Api POST "/admin/v1/stock/$sku/adjustments" @{delta=5} $stockHeaders
    $null = Api PUT "/admin/v1/products/$($product.id)/sale" @{onSale=$true} $adminHeaders
    $template = Api POST '/admin/v1/coupons' @{name="演示满30减5_$run";thresholdFen=3000;discountFen=500;total=5;validFrom=[DateTime]::UtcNow.AddMinutes(-1).ToString('o');validUntil=[DateTime]::UtcNow.AddDays(1).ToString('o')} $adminHeaders
    $coupon = Api POST "/api/v1/coupons/$($template.id)/claim" $null $memberHeaders
    $null = Api POST '/api/v1/cart' @{skuId=$sku;quantity=2} $memberHeaders
    $checkout = @{addressId=$address.id;couponId=$coupon.id;lines=@(@{skuId=$sku;quantity=2})}
    $quote = Api POST '/api/v1/checkout' $checkout $memberHeaders
    Assert-Equal $quote.goodsFen 3980 '商品金额'
    Assert-Equal $quote.discountFen 500 '优惠金额'
    $orderHeaders=$memberHeaders.Clone();$orderHeaders['Idempotency-Key']="order_$run"
    $place=@{checkout=$checkout;quoteFingerprint=$quote.fingerprint;fromCart=$true}
    $order=Api POST '/api/v1/orders' $place $orderHeaders
    $script:orderNo=$order.orderNo
    $replayed=Api POST '/api/v1/orders' $place $orderHeaders
    Assert-Equal $replayed.orderNo $order.orderNo '下单幂等'
    Write-Output "已下单：$script:orderNo，应付 $($quote.payableFen) 分"
    $payment=Api POST '/api/v1/payments' @{orderNo=$order.orderNo} $memberHeaders
    $null=Until '渠道下单' { Api GET "/api/v1/payments/$($payment.paymentNo)" $null $memberHeaders } { param($p) $null -ne $p.payParameters -and $p.payParameters.mode -eq 'LOCAL_SIMULATION' }
    $null=Api POST "/dev/v1/payments/$($payment.paymentNo)/success" @{copies=2;delaySeconds=0;failures=1} $devHeaders
    $null=Until '支付到账' { Api GET "/api/v1/orders/$script:orderNo" $null $memberHeaders } { param($o) $o.status -eq 'PAID' }
    $shipment=Api POST "/admin/v1/orders/$script:orderNo/shipment" @{carrier='模拟快递';trackingNo="send_$run"} $adminHeaders
    $null=Until '物流受理' { Api GET "/admin/v1/orders/$script:orderNo/shipment" $null $adminHeaders } { param($s) $null -ne $s -and $s.status -ne 'CREATING' }
    $null=Api POST "/dev/v1/shipments/$($shipment.shipmentNo)/tracks" @{eventId="delivered_$run";eventTime=[DateTime]::UtcNow.ToString('o');status='DELIVERED';description='买家已签收'} $devHeaders
    $null=Until '轨迹同步' { Api GET "/api/v1/orders/$script:orderNo/tracks" $null $memberHeaders } { param($tracks) @($tracks | Where-Object status -eq 'DELIVERED').Count -gt 0 }
    $null=Api POST "/api/v1/orders/$script:orderNo/receipt" $null $memberHeaders
    $after=Api POST '/api/v1/after-sales' @{orderNo=$order.orderNo;reason='学习整单退货退款'} $memberHeaders
    $null=Api POST "/admin/v1/after-sales/$($after.afterSaleNo)/review" @{approved=$true;reason='同意退货'} $adminHeaders
    $null=Api POST "/api/v1/after-sales/$($after.afterSaleNo)/return" @{carrier='模拟快递';trackingNo="return_$run"} $memberHeaders
    $null=Api POST "/admin/v1/after-sales/$($after.afterSaleNo)/accept-return" $null $adminHeaders
    $finished=Until '退款完成' { Api GET "/api/v1/after-sales/$($after.afterSaleNo)" $null $memberHeaders } { param($a) $a.status -eq 'COMPLETED' }
    $refund=Api GET "/api/v1/refunds/$($finished.refundNo)" $null $memberHeaders
    $final=Api GET "/api/v1/orders/$script:orderNo" $null $memberHeaders
    $stock=Api GET "/admin/v1/stock/$sku" $null $adminHeaders
    # Invoke-RestMethod 的 JSON 数组可能作为单个管道对象输出，先接收再枚举条目。
    $mine = Api GET '/api/v1/coupons/mine' $null $memberHeaders
    $used = @($mine | Where-Object { $_.id -eq $coupon.id })[0]
    Assert-Equal $final.status 'REFUNDED' '订单终态'
    Assert-Equal $refund.status 'SUCCEEDED' '退款终态'
    Assert-Equal $refund.amountFen $quote.payableFen '退款金额'
    Assert-Equal $stock.available 5 '退款返库'
    Assert-Equal $stock.reserved 0 '库存占用'
    Assert-Equal $used.status 'USED' '退款不返券'
    Write-Output "完整流程通过：订单 $script:orderNo，退款 $($refund.refundNo)。演示数据保留供学习。"
} catch {
    Write-Output "演示未通过。订单号：$script:orderNo；本次标识：$run。"
    throw
}
