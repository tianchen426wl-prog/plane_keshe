const escape = (value) =>
  String(value || "")
    .replace(/\\/g, "\\\\")
    .replace(/\r\n|\r|\n/g, "\\n")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,");
const stamp = (value) => {
  const date = new Date(value);
  if (!value || !Number.isFinite(date.getTime()))
    throw Error("行程时间不完整，暂时无法导出日历");
  return date
    .toISOString()
    .replace(/[-:]/g, "")
    .replace(/\.\d{3}Z$/, "Z");
};
// RFC 5545 limits physical lines to 75 UTF-8 octets; never split a character.
const fold = (line) => {
  const encoder = new TextEncoder();
  let result = "",
    segment = "";
  for (const character of line) {
    if (encoder.encode(segment + character).length > 75) {
      result += segment + "\r\n";
      segment = " ";
    }
    segment += character;
  }
  return result + segment;
};
export function calendarText(order) {
  const flight = order.flight || {};
  if (order.status !== 1 || flight.operationalStatus === "CANCELLED")
    throw Error("仅可为有效已购订单导出日历");
  if (new Date(flight.arrivalTime) <= new Date(flight.starttime))
    throw Error("行程抵达时间不完整，请核对后重试");
  return (
    [
      "BEGIN:VCALENDAR",
      "VERSION:2.0",
      "PRODID:-//AirTicket Course//Itinerary//ZH",
      "CALSCALE:GREGORIAN",
      "BEGIN:VEVENT",
      `UID:airticket-order-${Number(order.ddid)}@course.local`,
      `DTSTAMP:${stamp(order.ordertime)}`,
      `DTSTART:${stamp(flight.starttime)}`,
      `DTEND:${stamp(flight.arrivalTime)}`,
      `SUMMARY:${escape(
        "演示行程 " +
          flight.start +
          " → " +
          flight.end +
          " " +
          (flight.flightNo || "")
      )}`,
      `LOCATION:${escape(
        (flight.departureAirportName || flight.place || "") +
          " " +
          (flight.departureTerminal || "")
      )}`,
      `DESCRIPTION:${escape(
        "课程演示行程，不可用于真实乘机或报销。订单 " +
          order.orderNo +
          "。请在出发前重新核对航班信息。"
      )}`,
      "STATUS:CONFIRMED",
      "END:VEVENT",
      "END:VCALENDAR",
    ]
      .map(fold)
      .join("\r\n") + "\r\n"
  );
}
