import request from "./request";
export const getPassengers = () => request.get("/user/passengers");
export const savePassenger = (passenger) =>
  passenger.id
    ? request.put(`/user/passengers/${passenger.id}`, { name: passenger.name })
    : request.post("/user/passengers", { name: passenger.name });
export const deletePassenger = (id) => request.delete(`/user/passengers/${id}`);
