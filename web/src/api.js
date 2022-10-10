import { useRecoilState } from 'recoil';
import { atoms } from './atoms';

const GET = 'GET';
const POST = 'POST';
const DELETE = 'DELETE';

const Api = () => {
  const baseUri = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
  const [ , setStatus ] = useRecoilState(atoms.status);

  const apiCall = (method, uri, body, callback) => {
    const config = { method };
    if (body) {
      config.body = JSON.stringify(body);
      config.headers = { "Content-Type": "application/json" };
    }

    const filter = async (response) => {
      if (response.ok) {
        return response?.json() || response?.text();
      } else {
        throw await response.json();
      }
    };

    const error = (response) => {
      const { status, error, message } = response;
      if (status === 401) {
        window.location.href = baseUri + "/api/authorize";
        return;
      }
      setStatus({
        open: true,
        error: true,
        msg: `Error ${status || ""} ${error || ""}: ${message}`,
      });
      console.log(response);
    };

    fetch(baseUri + uri, config).then(filter).then(callback).catch(error);
  };

  return {
    searchInstrument: (value, callback) => apiCall(GET, `/api/search?query=${value.trim()}`, null, callback),
    getPortfolio: (callback) => apiCall(GET, '/api/portfolio', null, callback),
    addTx: (value, callback) => apiCall(POST, '/api/tx', value, callback),
    listTx: (callback) => apiCall(GET, '/api/tx', null, callback),
    deleteTx: (value, callback) => apiCall(DELETE, `/api/tx/${value}`, null, callback),
  };
};
export default Api;
