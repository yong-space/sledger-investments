import { useRecoilState } from 'recoil';
import { atoms } from './atoms';

const Api = () => {
  const baseUri = window.location.hostname === "localhost" ? "http://localhost:8080" : "";
  const [ , setStatus ] = useRecoilState(atoms.status);

  const apiCall = (uri, body, callback) => {
    const config = body && {
      method: "post",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    };

    const filter = async (response) => {
      if (response.ok) {
        return response?.json() || response?.text();
      } else {
        throw await response.json();
      }
    };

    const error = (response) => {
      const { status, error, message } = response;
      if (message.indexOf("401 Unauthorized") === 0) {
        window.location.href = baseUri + "/authorize";
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
    searchInstrument: (value, callback) => apiCall(`/search?query=${value.trim()}`, null, callback),
    getPortfolio: (callback) => apiCall('/portfolio', null, callback),
    addTx: (value, callback) => apiCall('/add-tx', value, callback),
    listTx: (value, callback) => apiCall('/tx', null, callback),
  };
};
export default Api;
