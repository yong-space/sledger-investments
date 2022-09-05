import CircularProgress from '@mui/material/CircularProgress';
import styled from "styled-components";

const LoaderRoot = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const Loader = () => (
  <LoaderRoot>
    <CircularProgress size="5rem" thickness={2} />
  </LoaderRoot>
);

export default Loader;
