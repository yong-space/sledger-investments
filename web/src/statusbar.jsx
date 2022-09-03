import { useRecoilState } from 'recoil';
import { atoms } from './atoms';
import Snackbar from '@mui/material/Snackbar';
import SnackbarContent from '@mui/material/SnackbarContent';

const StatusBar = () => {
  const [ status, setStatus ] = useRecoilState(atoms.status);
  const snackbarStyle = { color: "white", fontWeight: "bold" };

  return (
    <Snackbar
      open={status.open}
      onClose={() => setStatus({ open: false })}
      autoHideDuration={status.error ? 10000 : 3000}
    >
      <SnackbarContent
        style={{
          ...snackbarStyle,
          backgroundColor: status.error ? "#d74545" : "#4e9a51",
        }}
        message={status.msg}
      />
    </Snackbar>
  );
};
export default StatusBar;
