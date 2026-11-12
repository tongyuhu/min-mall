export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
export interface Entry {
  id: string;
  parentId: string | null;
  name: string;
}
export interface Sku {
  id: string;
  specs: string;
  priceFen: number;
  enabled: boolean;
}
export interface Product {
  id: string;
  categoryId: string;
  brandId: string | null;
  title: string;
  onSale: boolean;
  imageUrls: string[];
  skus: Sku[];
}
export interface Address {
  receiver: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  defaultAddress: boolean;
}
export interface AddressView {
  id: string;
  address: Address;
}
export interface Member {
  id: string;
  username: string;
  nickname: string;
  enabled: boolean;
}
export interface CartItem {
  id: string;
  skuId: string;
  title: string;
  specs: string;
  priceFen: number;
  quantity: number;
  selected: boolean;
  valid: boolean;
}
export interface Coupon {
  id: string;
  templateId?: string;
  name: string;
  thresholdFen: number;
  discountFen: number;
  status?: string;
  remaining?: number;
  validFrom: string;
  validUntil: string;
  enabled?: boolean;
  total?: number;
  issued?: number;
}
export interface Checkout {
  addressId: string;
  lines: { skuId: string; quantity: number }[];
  couponId: string | null;
}
export interface OrderItem {
  skuId: string;
  title: string;
  specs: string;
  imageUrl: string;
  quantity: number;
  unitPriceFen: number;
  payableFen: number;
}
export interface Order {
  orderNo: string;
  status: string;
  goodsFen: number;
  discountFen: number;
  freightFen: number;
  payableFen: number;
  items: OrderItem[];
  address: Address;
  createdAt: string;
  expiresAt: string;
  paidAt: string;
  completedAt: string;
  activeAfterSaleNo: string | null;
  closeRequested: boolean;
}
export interface Quote {
  skus: { skuId: string; title: string; specs: string; priceFen: number }[];
  goodsFen: number;
  discountFen: number;
  freightFen: number;
  payableFen: number;
  fingerprint: string;
}
export interface Payment {
  paymentNo: string;
  orderNo: string;
  status: string;
  amountFen: number;
  payParameters: Record<string, string>;
}
export interface Track {
  eventId: string;
  eventTime: string;
  status: string;
  description: string;
}
export interface Shipment {
  shipmentNo: string;
  orderNo: string;
  carrier: string;
  trackingNo: string;
  status: string;
}
export interface AfterSale {
  id: string;
  afterSaleNo: string;
  orderNo: string;
  type: string;
  status: string;
  reason: string;
  returnCarrier: string;
  returnTrackingNo: string;
  refundNo: string | null;
  createdAt: string;
}
export interface Refund {
  refundNo: string;
  orderNo: string;
  amountFen: number;
  status: string;
  refundedAt: string | null;
  lastError: string | null;
}
export interface AdminProfile {
  id: string;
  username: string;
  permissions: string[];
}
export interface AdminAccount {
  id: string;
  username: string;
  enabled: boolean;
  roleIds: string[];
}
export interface Role {
  id: string;
  name: string;
  permissions: string[];
}
